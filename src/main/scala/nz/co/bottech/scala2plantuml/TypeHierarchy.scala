package nz.co.bottech.scala2plantuml

import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.meta.internal.semanticdb._
import scala.meta.internal.symtab.SymbolTable

final case class TypeHierarchy(symbolInformation: SymbolInformation, parents: Seq[TypeHierarchy]) {

  def subTypeOf(parent: String): Boolean = {
    @tailrec
    def loop(remaining: Seq[TypeHierarchy], seen: Set[String]): Boolean =
      remaining match {
        case Nil                                                          => false
        case head +: tail if seen.contains(head.symbolInformation.symbol) => loop(tail, seen)
        case head +: _ if head.symbolInformation.symbol == parent         => true
        case head +: tail                                                 => loop(head.parents ++ tail, seen + head.symbolInformation.symbol)
      }
    loop(parents, Set.empty)
  }
}

object TypeHierarchy {

  private val logger = LoggerFactory.getLogger(classOf[TypeHierarchy])

  def create(symbolInformation: SymbolInformation, symbolTable: SymbolTable): TypeHierarchy = {
    val parentTypes = symbolInformation.signature match {
      case classSignature: ClassSignature => classSignature.parents
      case typeSignature: TypeSignature   => List(typeSignature.upperBound)
      case _                              => Nil
    }
    val parentTypeRefs = parentTypes.collect {
      case typeRef: TypeRef => typeRef
    }
    val parentSymbols     = parentTypeRefs.map(lookupSymbol(_, symbolTable))
    val parentHierarchies = parentSymbols.map(create(_, symbolTable))
    TypeHierarchy(symbolInformation, parentHierarchies)
  }

  private def lookupSymbol(typeRef: TypeRef, symbolTable: SymbolTable): SymbolInformation = {
    val symbol = typeRef.symbol
    symbolTable.info(symbol).getOrElse {
      logger.warn(s"Missing symbol for $symbol")
      missingSymbol(symbol)
    }
  }

  private def missingSymbol(symbol: String): SymbolInformation = SymbolInformation(symbol = symbol)
}
