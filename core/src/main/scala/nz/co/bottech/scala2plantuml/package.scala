package nz.co.bottech

import java.util.regex.Pattern
import scala.meta.internal.semanticdb.Scala._
import scala.util.matching.Regex

package object scala2plantuml {

  private[scala2plantuml] val JavaStdLibPattern: String  = "java/**"
  private[scala2plantuml] val ScalaStdLibPattern: String = "scala/**"

  private[scala2plantuml] def patternPredicate(pattern: String): String => Boolean = { (symbol: String) =>
    patternToRegex(pattern).matcher(symbol).matches()
  }

  private[scala2plantuml] def patternToRegex(pattern: String): Pattern =
    Pattern.compile(
      pattern
        .split("""\*\*""", -1)
        .map(_.split("""\*""", -1).map(Regex.quote).mkString("""[^/]*"""))
        .mkString(""".*""")
    )

  private[scala2plantuml] def stdLibSymbol(symbol: String): Boolean =
    scalaStdLibSymbol(symbol) || javaStdLibSymbol(symbol)

  private[scala2plantuml] def javaStdLibSymbol(symbol: String): Boolean =
    symbol.startsWith("java/")

  private[scala2plantuml] def scalaStdLibSymbol(symbol: String): Boolean =
    symbol.startsWith("scala/")

  private[scala2plantuml] def symbolToScalaIdentifier(symbol: String): String =
    if (symbol.isTypeParameter || symbol.isParameter) symbol.desc.value
    // Don't use the descriptor value for other globals as we need the fully qualified name.
    else if (symbol.isGlobal) toScalaSeparators(symbol.init)
    else toScalaSeparators(symbol)

  private def toScalaSeparators(symbol: String): String =
    symbol.replace('.', '$').replace('/', '.')

  private[scala2plantuml] def symbolOwner(symbol: String): String =
    symbol.ownerChain.takeRight(2).headOption.getOrElse(symbol)

  private[scala2plantuml] def scalaTypeName(identifier: String): String =
    identifier.split('.').last.split('#').head

}
