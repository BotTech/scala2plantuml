package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramPrinter.Options
import utest.assert

trait ClassDiagramTest {

  protected val exampleDir: String

  private def globalSymbol(symbol: String) =
    s"nz/co/bottech/scala2plantuml/examples/$exampleDir/$symbol#"

  protected def success(name: String, diagram: String, options: Options = Options.default): Unit = {
    val result = generateFromTopLevel(name, options)
    assert(result == Right(diagram))
  }

  protected def generateFromTopLevel(symbol: String, options: Options): Either[String, String] =
    ClassDiagramGenerator
      .basedOn(
        globalSymbol(symbol),
        List("META-INF/semanticdb/src/test/scala"),
        this.getClass.getClassLoader
      )
      .map { elements =>
        ClassDiagramPrinter.printSnippet(elements, options)
      }
}
