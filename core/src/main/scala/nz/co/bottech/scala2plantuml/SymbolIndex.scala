package nz.co.bottech.scala2plantuml

import scala.meta.internal.semanticdb.Scala._
import scala.meta.internal.semanticdb.SymbolInformation
import scala.meta.internal.symtab.SymbolTable

class SymbolIndex(ignore: String => Boolean, symbolTable: SymbolTable) {

  def indexOf(symbol: String): Boolean = symbol.isGlobal && !ignore(symbol)

  def lookup(symbol: String): Option[SymbolInformation] =
    // Only global symbols can be found in the symbol table.
    if (indexOf(symbol)) symbolTable.info(symbol)
    else None
}
