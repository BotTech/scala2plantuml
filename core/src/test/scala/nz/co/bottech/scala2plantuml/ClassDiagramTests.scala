package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramRenderer.Options
import utest.assert

trait ClassDiagramTests {

  protected val testOptions: Options = Options.Minimal

  protected val exampleDir: String

  private def globalSymbol(symbol: String) =
    s"nz/co/bottech/scala2plantuml/examples/$exampleDir/$symbol"

  protected def success(name: String, expected: String, options: Options = testOptions): Unit = {
    val result = generateFromTopLevel(name, options).trim
    assert(result == expected)
  }

  protected def generateFromTopLevel(symbol: String, options: Options): String = {
    val elements = ClassDiagramGenerator
      .fromSymbol(
        globalSymbol(symbol),
        List("META-INF/semanticdb/core/src/test/scala"),
        // TODO: Add tests where we don't ignore these.
        stdLibSymbol,
        this.getClass.getClassLoader
      )
    ClassDiagramRenderer.renderSnippet(elements, options)
  }
}
