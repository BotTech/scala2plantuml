package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramRenderer.Options
import utest.assert

trait ClassDiagramTests {

  protected val testOptions: Options = Options.Minimal

  protected val exampleDir: String

  private def globalSymbol(symbol: String) =
    s"nz/co/bottech/scala2plantuml/examples/$exampleDir/$symbol"

  protected def success(
      name: String,
      expected: String,
      maxLevel: Option[Int] = None,
      options: Options = testOptions
    ): Unit = {
    val result = generateFromTopLevel(name, maxLevel, options).trim
    assert(result == expected)
  }

  protected def generateFromTopLevel(symbol: String, maxLevel: Option[Int], options: Options): String = {
    val elements = ClassDiagramGenerator
      .fromSymbol(
        globalSymbol(symbol),
        List("META-INF/semanticdb/core/src/test/scala"),
        stdLibSymbol,
        this.getClass.getClassLoader,
        maxLevel
      )
    ClassDiagramRenderer.renderSnippetString(elements, options)
  }
}
