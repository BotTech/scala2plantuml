package nz.co.bottech.scala2plantuml

import scala.meta._

object ClassDiagramGenerator {

  def generate(scalaCode: String): Either[String, String] =
    scalaCode.parse[Source].toEither.map(sourceToDiagram).left.map(_.toString)

  private def sourceToDiagram(source: Source): String = {
    val traverser = new SourceTraverser()
    traverser(source)
    ClassDiagramPrinter.print(traverser.result())
  }
}
