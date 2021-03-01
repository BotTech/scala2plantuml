package nz.co.bottech.scala2plantuml

import utest.{test, TestSuite, Tests}

object MethodTypeParameterTests extends TestSuite with ClassDiagramTests {

  override protected val exampleDir: String = "methodtypeparameters"

  val tests: Tests = Tests {
    test("single type parameter") {
      success(
        "SingleTypeParameter#",
        """interface SingleTypeParameter {
          |  + {abstract} {method} foo
          |}""".stripMargin
      )
    }
    test("type parameter with bounds") {
      success(
        "TypeParameterWithBounds#",
        """interface Trait
          |interface TypeParameterWithBounds {
          |  + {abstract} {method} foo
          |}
          |TypeParameterWithBounds o-- Trait""".stripMargin
      )
    }
    test("type parameter with multiple bounds") {
      success(
        "TypeParameterWithMultipleBounds#",
        """interface Trait
          |interface Trait2
          |interface TypeParameterWithMultipleBounds {
          |  + {abstract} {method} foo
          |}
          |TypeParameterWithMultipleBounds o-- Trait
          |TypeParameterWithMultipleBounds o-- Trait2""".stripMargin
      )
    }
    test("type parameter with extension") {
      success(
        "TypeParameterWithExtension#",
        """interface Trait
          |interface TypeParameterWithExtension<A extends Trait> {
          |  + {abstract} {method} foo
          |}
          |TypeParameterWithExtension o-- Trait""".stripMargin
      )
    }
  }
}
