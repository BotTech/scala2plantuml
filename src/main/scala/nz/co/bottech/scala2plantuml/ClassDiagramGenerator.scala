package nz.co.bottech.scala2plantuml

import java.io.InputStream
import java.nio.charset.{Charset, StandardCharsets}

object ClassDiagramGenerator {

  def fromInputStream(
      source: InputStream,
      semanticDb: InputStream,
      charset: Charset = StandardCharsets.UTF_8
    ): Either[String, String] =
    for {
      syntactic <- SyntacticProcessor.processInputStream(source, charset)
      semantic  <- SemanticProcessor.processInputStream(semanticDb)
    } yield ClassDiagramPrinter.print(syntactic ++ semantic)
}
