package nz.co.bottech.scala2plantuml

import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.meta.internal.semanticdb._

final case class TypeHierarchy(symbolInformation: SymbolInformation, parents: List[TypeHierarchy]) {

  def subTypeOf(parent: String): Boolean = {
    @tailrec
    def loop(remaining: List[TypeHierarchy], seen: Set[String]): Boolean =
      remaining match {
        case Nil                                                          => false
        case head :: tail if seen.contains(head.symbolInformation.symbol) => loop(tail, seen)
        case head :: _ if head.symbolInformation.symbol == parent         => true
        case head :: tail                                                 => loop(tail ++ remaining, seen + head.symbolInformation.symbol)
      }
    loop(parents, Set.empty)
  }
}

object TypeHierarchy {

  private val logger = LoggerFactory.getLogger(classOf[TypeHierarchy])

  def create(symbolInformation: SymbolInformation, index: SymbolIndex): Indexed[TypeHierarchy] = {
    val parents = symbolInformation.signature match {
      case classSignature: ClassSignature => classSignature.parents
      case typeSignature: TypeSignature   => List(typeSignature.upperBound)
      case _                              => Nil
    }
    val indexedParentSymbols     = lookupParentSymbols(parents, index)
    val indexedParentHierarchies = typeHierarchies(indexedParentSymbols.value, indexedParentSymbols.index)
    indexedParentHierarchies.map(TypeHierarchy(symbolInformation, _))
  }

  private def typeHierarchies(symbols: List[SymbolInformation], index: SymbolIndex): Indexed[List[TypeHierarchy]] =
    symbols.reverse.foldLeft(Indexed(List.empty[TypeHierarchy], index)) {
      case (acc, parentSymbol) =>
        val indexedHierarchy = create(parentSymbol, acc.index)
        indexedHierarchy.map(_ +: acc.value)
    }

  private def lookupParentSymbols(parents: Seq[Type], index: SymbolIndex): Indexed[List[SymbolInformation]] =
    parents.reverse.foldLeft(Indexed(List.empty[SymbolInformation], index)) {
      case (acc, parent: TypeRef) =>
        acc.lookup(parent.symbol) match {
          case Some(parentSymbol) => acc.map(parentSymbol +: _)
          case None =>
            val symbol = SymbolIndex.missingSymbol(parent.symbol)
            logger.warn(s"Missing symbol for $symbol")
            acc.map(symbol +: _).addToIndex(parent.symbol, symbol)
        }
      case (accIndex, _) => accIndex
    }
}
