package nz.co.bottech.scala2plantuml

import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.meta.internal.semanticdb.{ClassSignature, SymbolInformation, TypeRef, TypeSignature}
import scala.meta.internal.symtab.SymbolTable

class TypeIndex(symbolTable: SymbolTable) {

  private val logger = LoggerFactory.getLogger(classOf[TypeIndex])
  private val cache  = TrieMap.empty[String, TypeHierarchy]

  def getHierarchy(symbolInformation: SymbolInformation): TypeHierarchy =
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
        val parentSymbols     = parentTypeRefs.map(lookupSymbol)
        val parentHierarchies = parentSymbols.map(getHierarchy)
        TypeHierarchy(symbolInformation, parentHierarchies)
      }
    )

  private def lookupSymbol(typeRef: TypeRef): SymbolInformation = {
    val symbol = typeRef.symbol
    symbolTable.info(symbol).getOrElse {
      logger.warn(s"Missing symbol for $symbol")
      missingSymbol(symbol)
    }
  }

  private def missingSymbol(symbol: String): SymbolInformation = SymbolInformation(symbol = symbol)
}
