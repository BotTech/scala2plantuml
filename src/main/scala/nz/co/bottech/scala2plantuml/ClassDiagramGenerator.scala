package nz.co.bottech.scala2plantuml

import scala.meta._

object ClassDiagramGenerator {

  def generate(scalaCode: String): Either[String, String] = {
    scalaCode.parse[Source].toEither.map(sourceToDiagram).left.map(_.toString)
  }

  private def sourceToDiagram(source: Source): String = {
    val traverser = new SourceTraverser()
    traverser(source)
    ClassDiagramPrinter.print(traverser.result())
  }

  private def debug(source: Source): String = {
    source.collect {
      case node => s"${node.productPrefix}: ${node.toString}"
    }.mkString("\n")
  }
}
