package nz.co.bottech.scala2plantuml

import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.meta.internal.semanticdb.{ClassSignature, SymbolInformation, TypeRef, TypeSignature}
import scala.meta.internal.symtab.SymbolTable

private[scala2plantuml] class TypeIndex(symbolTable: SymbolTable) {

  private val logger = LoggerFactory.getLogger(classOf[TypeIndex])
  private val cache  = TrieMap.empty[String, TypeHierarchy]

  def hierarchy(symbolInformation: SymbolInformation, ignore: String => Boolean): TypeHierarchy =
    cache.getOrElseUpdate(
      symbolInformation.symbol, {
        val parentTypes = symbolInformation.signature match {
          case classSignature: ClassSignature => classSignature.parents
          case typeSignature: TypeSignature   => List(typeSignature.upperBound)
          case _                              => Nil
        }
        val parentTypeRefs = parentTypes.collect {
          case typeRef: TypeRef => typeRef
        }
        val parentSymbols     = parentTypeRefs.map(lookupSymbol(_, ignore))
        val parentHierarchies = parentSymbols.map(hierarchy(_, ignore))
        TypeHierarchy(symbolInformation, parentHierarchies)
      }
    )

  private def lookupSymbol(typeRef: TypeRef, ignore: String => Boolean): SymbolInformation = {
    val symbol = typeRef.symbol
    if (ignore(symbol)) emptySymbol(symbol)
    else
      symbolTable.info(symbol).getOrElse {
        logger.warn(s"Missing symbol: $symbol")
        emptySymbol(symbol)
      }
  }

  private def emptySymbol(symbol: String): SymbolInformation = SymbolInformation(symbol = symbol)
}
