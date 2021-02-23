package nz.co.bottech.scala2plantuml

import utest.assert

trait GeneratorTest {

  protected val exampleDir: String

  private def globalSymbol(symbol: String) =
    s"nz/co/bottech/scala2plantuml/examples/$exampleDir/$symbol#"

  protected def success(name: String, diagram: String): Unit = {
    val result = generateFromTopLevel(name)
    assert(result == Right(diagram))
  }

  protected def generateFromTopLevel(symbol: String): Either[String, String] =
    ClassDiagramGenerator.basedOn(
      globalSymbol(symbol),
      List("META-INF/semanticdb/src/test/scala"),
      this.getClass.getClassLoader
    )
}
