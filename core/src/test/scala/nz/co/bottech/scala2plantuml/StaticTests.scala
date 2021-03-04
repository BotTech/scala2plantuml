package nz.co.bottech.scala2plantuml

import utest.{test, TestSuite, Tests}

object StaticTests extends TestSuite with ClassDiagramTests {

  override protected val exampleDir: String = "static"

  val tests: Tests = Tests {
    test("static field") {
      success(
        "StaticField.",
        """class StaticField {
          |  + {static} {field} field
          |}""".stripMargin
      )
    }
    test("static method") {
      success(
        "StaticMethod.",
        """class StaticMethod {
          |  + {static} {method} method
          |}""".stripMargin
      )
    }
  }
}
