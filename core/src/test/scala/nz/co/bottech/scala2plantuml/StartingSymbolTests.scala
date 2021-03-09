package nz.co.bottech.scala2plantuml

import utest.{test, TestSuite, Tests}

object StartingSymbolTests extends TestSuite with ClassDiagramTests {

  override protected val exampleDir: String = "start"

  val tests: Tests = Tests {
    test("method") {
      success(
        "Foo.apply().",
        """class Foo {
          |  + {static} {method} apply
          |}""".stripMargin
      )
    }
  }
}
