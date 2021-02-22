package nz.co.bottech.scala2plantuml

import java.io.InputStream

object ClassDiagramGenerator {

  def fromInputStream(
      semanticDb: InputStream
    ): Either[String, String] =
    for {
      semantic <- SemanticProcessor.processInputStream(semanticDb)
    } yield ClassDiagramPrinter.print(semantic)
}
