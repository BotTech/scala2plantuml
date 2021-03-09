package nz.co.bottech.scala2plantuml

import utest.{TestSuite, Tests, test}

object MethodParameterTests extends TestSuite with ClassDiagramTests {

  override protected val exampleDir: String = "methodparameters"

  val tests: Tests = Tests {
    test("parameter with type alias") {
      success(
        "Foo.",
        """class Foo {
          |  + {static} {method} foo
          |}
          |Foo o-- "package$Bar"""".stripMargin
      )
    }
  }
}
