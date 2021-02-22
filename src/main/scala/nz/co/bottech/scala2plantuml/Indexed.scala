package nz.co.bottech.scala2plantuml

import scala.meta.internal.semanticdb.SymbolInformation

final case class Indexed[A](value: A, index: SymbolIndex) {

  def map[B](f: A => B): Indexed[B] = copy(value = f(value))

  def lookup(symbol: String): Option[SymbolInformation] = index.lookup(symbol)

  def addToIndex(symbol: String, symbolInformation: SymbolInformation): Indexed[A] =
    copy(index = index.add(symbol, symbolInformation))
}
