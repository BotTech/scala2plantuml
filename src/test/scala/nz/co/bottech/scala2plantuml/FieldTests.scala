package nz.co.bottech.scala2plantuml

import utest.{TestSuite, Tests, test}

object FieldTests extends TestSuite with ClassDiagramTest {

  override protected val exampleDir: String = "field"

  val tests: Tests = Tests {
    test("public field") {
      success("PublicField",
        """class PublicField {
          |  + {field} field
          |}""".stripMargin)
    }
  }
}
