package nz.co.bottech.scala2plantuml.sbt

import nz.co.bottech.scala2plantuml.symbolToScalaIdentifier
import sbt.complete.DefaultParsers._
import sbt.complete.Parser
import sbt.file

private[sbt] object Scala2PlantUMLParser {

  private val SymbolParser = token(StringBasic, "symbol")

  private val IncludeParser    = optionStringParser("-i", "--include", "pattern", _.addInclude(_))
  private val ExcludeParser    = optionStringParser("-e", "--exclude", "pattern", _.addExclude(_))
  private val OutputFileParser = optionStringParser("-o", "--output", "file", _.replaceOutputFile(_))

  lazy val DigitSet = Set("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")

  private val PositiveDigit = charClass(c => c.isDigit && c.toInt > 0, "positive digit").examples(DigitSet)

  private val PosIntParser = mapOrFail(PositiveDigit ~ Digit.*)({ case (first, others) =>
    (first +: others).mkString.toInt
  })

  private val MaxLevelParser = optionPosIntParser("-l", "--max-level", "level", _.setMaxLevel(_))

  private def optionStringParser(short: String, long: String, label: String, f: (Config, String) => Config) =
    optionParser(short, long, label, StringBasic, f)

  //noinspection SameParameterValue
  private def optionPosIntParser(short: String, long: String, label: String, f: (Config, Int) => Config) =
    optionParser(short, long, label, PosIntParser, f)

  private def optionParser[A](
      short: String,
      long: String,
      label: String,
      parser: Parser[A],
      f: (Config, A) => Config
    ) =
    (token(literal(short) | long) ~> token(Space) ~> token(parser, label)).map(a => (c: Config) => f(c, a))

  def apply(): Parser[Config] = {
    // TODO: Add .puml extension auto complete.
    val options = OutputFileParser | MaxLevelParser | IncludeParser | ExcludeParser
    // TODO: Get example symbols.
    // FIXME: OutputFileParser and MaxLevelParser should only be allowed at most once.
    val parser = Space ~> options.* ~ SymbolParser ~ options.*
    parser.map { case ((optionsBefore, symbol), optionsAfter) =>
      def configure(config: Config, f: Config => Config) = f(config)
      val outputFile                                     = file(s"${symbolToScalaIdentifier(symbol)}.puml")
      val initialConfig                                  = Config(symbol, outputFile)
      val config                                         = optionsBefore.foldLeft(initialConfig)(configure)
      optionsAfter.foldLeft(config)(configure)
    }
  }
}
