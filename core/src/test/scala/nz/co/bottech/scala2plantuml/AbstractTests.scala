package nz.co.bottech.scala2plantuml

import utest.{test, TestSuite, Tests}

object AbstractTests extends TestSuite with ClassDiagramTests {

  override protected val exampleDir: String = "abstract"

  val tests: Tests = Tests {
    test("abstract class field") {
      success(
        "AbstractClassField#",
        """abstract class AbstractClassField {
          |  + {abstract} {field} field
          |}""".stripMargin
      )
    }
    test("abstract class method") {
      success(
        "AbstractClassMethod#",
        """abstract class AbstractClassMethod {
          |  + {abstract} {method} method
          |}""".stripMargin
      )
    }
    test("abstract trait field") {
      success(
        "AbstractTraitField#",
        """interface AbstractTraitField {
          |  + {abstract} {field} field
          |}""".stripMargin
      )
    }
    test("abstract trait method") {
      success(
        "AbstractTraitMethod#",
        """interface AbstractTraitMethod {
          |  + {abstract} {method} method
          |}""".stripMargin
      )
    }
  }
}
