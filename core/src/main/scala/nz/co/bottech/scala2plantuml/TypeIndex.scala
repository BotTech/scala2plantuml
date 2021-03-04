package nz.co.bottech.scala2plantuml

import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.meta.internal.semanticdb.{ClassSignature, SymbolInformation, TypeRef, TypeSignature}

private[scala2plantuml] class TypeIndex(symbolIndex: SymbolIndex) {

  private val logger = LoggerFactory.getLogger(classOf[TypeIndex])
  private val cache  = TrieMap.empty[String, TypeHierarchy]

  def hierarchy(symbolInformation: SymbolInformation): TypeHierarchy =
    cache.getOrElseUpdate(
      symbolInformation.symbol, {
        val parentTypes = symbolInformation.signature match {
          case classSignature: ClassSignature => classSignature.parents
          case typeSignature: TypeSignature   => List(typeSignature.upperBound)
          case _                              => Nil
        }
        val parentTypeRefs = parentTypes.collect { case typeRef: TypeRef =>
          typeRef
        }
        val parentSymbols     = parentTypeRefs.map(lookupSymbol)
        val parentHierarchies = parentSymbols.map(hierarchy)
        TypeHierarchy(symbolInformation, parentHierarchies)
      }
    )

  private def lookupSymbol(typeRef: TypeRef): SymbolInformation = {
    val symbol = typeRef.symbol
    if (symbolIndex.indexOf(symbol))
      symbolIndex.lookup(symbol).getOrElse {
        logger.warn(s"Missing symbol: $symbol")
        emptySymbol(symbol)
      }
    else emptySymbol(symbol)
  }

  private def emptySymbol(symbol: String): SymbolInformation = SymbolInformation(symbol = symbol)
}
