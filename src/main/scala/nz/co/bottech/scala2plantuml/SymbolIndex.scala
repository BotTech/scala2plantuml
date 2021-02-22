package nz.co.bottech.scala2plantuml

import scala.meta.internal.semanticdb.SymbolInformation

class SymbolIndex(private val index: Map[String, SymbolInformation]) {

  def lookup(symbol: String): Option[SymbolInformation] = index.get(symbol)

  def add(symbol: String, symbolInformation: SymbolInformation): SymbolIndex =
    new SymbolIndex(index + (symbol -> symbolInformation))
}

object SymbolIndex {

  def empty = new SymbolIndex(Map.empty)

  def missingSymbol(symbol: String): SymbolInformation =
    SymbolInformation(symbol = symbol)
}
