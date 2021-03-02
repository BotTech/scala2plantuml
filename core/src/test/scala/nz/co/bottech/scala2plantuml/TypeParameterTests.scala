package nz.co.bottech.scala2plantuml

import utest.{test, TestSuite, Tests}

object TypeParameterTests extends TestSuite with ClassDiagramTests {

  override protected val exampleDir: String = "typeparameters"

  val tests: Tests = Tests {
//    test("single type parameter") {
//      success("SingleTypeParameter#", """interface SingleTypeParameter<A>""".stripMargin)
//    }
//    test("multiple type parameter") {
//      success("MultipleTypeParameters#", """interface MultipleTypeParameters<A, B>""".stripMargin)
//    }
//    test("type parameter with bounds") {
//      success(
//        "TypeParameterWithBounds#",
//        """interface Trait
//          |interface TypeParameterWithBounds<A extends Trait>
//          |TypeParameterWithBounds o-- Trait""".stripMargin
//      )
//    }
//    test("type parameter with bounds of another type parameter") {
//      success(
//        "TypeParameterWithParamBounds#",
//        """interface Trait
//          |interface TypeParameterWithParamBounds<A, B>""".stripMargin
//      )
//    }
//    test("type parameter with multiple bounds") {
//      success(
//        "TypeParameterWithMultipleBounds#",
//        """interface Trait
//          |interface Trait2
//          |interface TypeParameterWithMultipleBounds<A extends Trait & Trait2>
//          |TypeParameterWithMultipleBounds o-- Trait
//          |TypeParameterWithMultipleBounds o-- Trait2""".stripMargin
//      )
//    }
//    test("type parameter with extension") {
//      success(
//        "TypeParameterWithExtension#",
//        """interface Trait
//          |interface TypeParameterWithExtension<A> extends Trait""".stripMargin
//      )
//    }
    test("higher kinded type parameter") {
      success(
        "HigherKindedTypeParameter#",
        """interface HigherKindedTypeParameter<A>
          |HigherKindedTypeParameter o-- Trait
          |interface Trait""".stripMargin
      )
    }
  }
}
