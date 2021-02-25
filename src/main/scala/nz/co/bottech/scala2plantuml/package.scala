package nz.co.bottech

package object scala2plantuml {

  private[scala2plantuml] def scalaStdLibSymbol(symbol: String): Boolean =
    symbol.startsWith("scala/")
}
