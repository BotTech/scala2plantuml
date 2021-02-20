package nz.co.bottech.scala2plantuml

import java.io.InputStream
import java.nio.charset.{Charset, StandardCharsets}
import scala.meta._

object SyntacticProcessor {

  def processInputStream(
      inputStream: InputStream,
      charset: Charset = StandardCharsets.UTF_8
    ): Either[String, SourceSyntax] = {
    val input = Input.Stream(inputStream, charset)
    processInput(input)
  }

  private def processInput(input: Input): Either[String, SourceSyntax] =
    input.parse[Source].toEither.map(processSource).left.map(_.toString)

  private def processSource(source: Source): SourceSyntax = {
    val traverser = new SourceTraverser()
    traverser(source)
    traverser.result()
  }
}
