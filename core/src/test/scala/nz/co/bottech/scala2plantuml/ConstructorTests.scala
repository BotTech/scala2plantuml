package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramPrinter.Options
import utest.{test, TestSuite, Tests}

object ConstructorTests extends TestSuite with ClassDiagramTest {

  override protected val exampleDir: String = "constructor"

  val tests: Tests = Tests {
    test("hide constructor") {
      success(
        "EmptyConstructor#",
        """class EmptyConstructor""".stripMargin,
        options = testOptions.copy(constructor = Options.HideConstructors)
      )
    }
    test("show constructor") {
      success(
        "EmptyConstructor#",
        """class EmptyConstructor {
          |  + {method} <init>
          |}""".stripMargin,
        options = testOptions.copy(constructor = Options.ShowConstructors())
      )
    }
    test("constructor type name") {
      success(
        "EmptyConstructor#",
        """class EmptyConstructor {
          |  + {method} EmptyConstructor
          |}""".stripMargin,
        options = testOptions.copy(constructor = Options.ShowConstructors(name = Options.constructorTypeName))
      )
    }
    test("constructor stereotype") {
      success(
        "EmptyConstructor#",
        """class EmptyConstructor {
          |  + {method} <<Create>> <init>
          |}""".stripMargin,
        options = testOptions.copy(constructor = Options.ShowConstructors(stereotype = Options.CreateStereotype))
      )
    }
  }
}
