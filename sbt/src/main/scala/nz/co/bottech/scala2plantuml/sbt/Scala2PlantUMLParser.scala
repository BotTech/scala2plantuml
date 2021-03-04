package nz.co.bottech.scala2plantuml.sbt

import nz.co.bottech.scala2plantuml.symbolToScalaIdentifier
import sbt.complete.DefaultParsers._
import sbt.complete.Parser
import sbt.file

private[sbt] object Scala2PlantUMLParser {

  private val SymbolParser = token(StringBasic, "symbol")

  private val IncludeParser    = optionParser("-i", "--include", "pattern", _.addInclude(_))
  private val ExcludeParser    = optionParser("-e", "--exclude", "pattern", _.addExclude(_))
  private val OutputFileParser = optionParser("-o", "--output", "file", _.replaceOutputFile(_))

  private def optionParser(short: String, long: String, label: String, f: (Config, String) => Config) =
    (token(literal(short) | long) ~> token(Space) ~> token(StringBasic, label)).map(s => (c: Config) => f(c, s))

  def apply(): Parser[Config] = {
    // TODO: Add .puml extension auto complete.
    val options = OutputFileParser | IncludeParser | ExcludeParser
    // TODO: Get example symbols.
    val parser = Space ~> options.* ~ SymbolParser ~ options.*
    parser.map {
      case ((optionsBefore, symbol), optionsAfter) =>
        def configure(config: Config, f: Config => Config) = f(config)
        val outputFile                                     = file(s"${symbolToScalaIdentifier(symbol)}.puml")
        val initialConfig                                  = Config(symbol, outputFile)
        val config                                         = optionsBefore.foldLeft(initialConfig)(configure)
        optionsAfter.foldLeft(config)(configure)
    }
  }
}
