package nz.co.bottech

import java.util.regex.Pattern
import scala.util.matching.Regex

package object scala2plantuml {

  private[scala2plantuml] def stdLibSymbol(symbol: String): Boolean =
    scalaStdLibSymbol(symbol) || javaStdLibSymbol(symbol)

  private[scala2plantuml] def javaStdLibSymbol(symbol: String): Boolean =
    symbol.startsWith("java/")

  private[scala2plantuml] def scalaStdLibSymbol(symbol: String): Boolean =
    symbol.startsWith("scala/")

  private[scala2plantuml] def patternToRegex(pattern: String): Pattern =
    Pattern.compile(
      pattern
        .split("""\*\*""", -1)
        .map(_.split("""\*""", -1).map(Regex.quote).mkString("""[^/]*"""))
        .mkString(""".*""")
    )
}
