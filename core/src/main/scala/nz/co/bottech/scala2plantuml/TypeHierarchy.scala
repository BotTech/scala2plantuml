package nz.co.bottech.scala2plantuml

import scala.annotation.tailrec
import scala.meta.internal.semanticdb._

final private[scala2plantuml] case class TypeHierarchy(
    symbolInformation: SymbolInformation,
    parents: Seq[TypeHierarchy]) {

  def subTypeOf(parent: String): Boolean = {
    @tailrec
    def loop(remaining: Seq[TypeHierarchy], seen: Set[String]): Boolean =
      remaining match {
        case head +: tail if seen.contains(head.symbolInformation.symbol) => loop(tail, seen)
        case head +: _ if head.symbolInformation.symbol == parent         => true
        case head +: tail                                                 => loop(head.parents ++ tail, seen + head.symbolInformation.symbol)
        case _                                                            => false
      }
    loop(parents, Set.empty)
  }
}
