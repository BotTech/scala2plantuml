package nz.co.bottech.scala2plantuml

import utest.{test, TestSuite, Tests}

object ExtensionTests extends TestSuite with ClassDiagramTests {

  override protected val exampleDir: String = "extension"

  val tests: Tests = Tests {
//    test("single class extension") {
//      success("SingleClassExtension#", """class BaseClass
//                                        |class SingleClassExtension extends BaseClass""".stripMargin)
//    }
//    test("single trait extension") {
//      success(
//        "SingleTraitExtension#",
//        """interface BaseTrait
//          |interface SingleTraitExtension extends BaseTrait""".stripMargin
//      )
//    }
//    test("multiple class extension") {
//      success(
//        "MultipleClassExtension#",
//        """class BaseClass
//          |interface BaseTrait
//          |class MultipleClassExtension extends BaseClass, BaseTrait""".stripMargin
//      )
//    }
    test("higher-kinded extension") {
      success(
        "HigherKindedExtension#",
        """interface BaseTrait
          |interface HigherKindedExtension extends HigherKindedTrait
          |HigherKindedExtension o-- BaseTrait
          |interface HigherKindedTrait<A>""".stripMargin
      )
    }
    test("higher-kinded self extension") {
      success(
        "HigherKindedSelfExtension#",
        """interface HigherKindedSelfExtension extends HigherKindedTrait
          |interface HigherKindedTrait<A>""".stripMargin
      )
    }
    test("hidden higher-kinded extension") {
      success(
        "HiddenHigherKindedExtension#",
        """interface BaseTrait
          |interface HiddenHigherKindedExtension
          |HiddenHigherKindedExtension o-- BaseTrait""".stripMargin
      )
    }
    test("hidden higher-kinded self extension") {
      success(
        "HiddenHigherKindedSelfExtension#",
        """interface HiddenHigherKindedSelfExtension""".stripMargin
      )
    }
  }
}
