package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramRenderer.Options
import utest.{test, TestSuite, Tests}

object NestedTypeTests extends TestSuite with ClassDiagramTests {

  override protected val exampleDir: String = "nested"

  val tests: Tests = Tests {
    test("nested class") {
      success(
        "OuterClass#",
        """class OuterClass
          |class "OuterClass#InnerClass"""".stripMargin
      )
    }
    test("nested object") {
      success(
        "OuterObject.",
        """class OuterObject
          |class "OuterObject$InnerClass"""".stripMargin
      )
    }
    test("nested class with fields sorted") {
      success(
        "OuterClassWithFields#",
        """class OuterClassWithFields {
          |  + {field} a
          |  + {field} c
          |}
          |class "OuterClassWithFields#InnerClass" {
          |  + {field} b
          |}""".stripMargin
      )
    }
    // TODO: This should go in a separate suite of tests regarding ordering.
    test("nested class with fields unsorted") {
      success(
        "OuterClassWithFields#",
        """class OuterClassWithFields {
          |  + {field} a
          |}
          |class "OuterClassWithFields#InnerClass" {
          |  + {field} b
          |}
          |class OuterClassWithFields {
          |  + {field} c
          |}""".stripMargin,
        options = testOptions.copy(sorting = Options.Unsorted)
      )
    }
  }
}
