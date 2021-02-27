package nz.co.bottech.scala2plantuml

import utest.{test, TestSuite, Tests}

object ExtensionTests extends TestSuite with ClassDiagramTests {

  override protected val exampleDir: String = "extension"

  val tests: Tests = Tests {
    test("single class extension") {
      success("SingleClassExtension#", """class BaseClass
                                        |class SingleClassExtension extends BaseClass""".stripMargin)
    }
    test("single trait extension") {
      success("SingleTraitExtension#", """interface BaseTrait
                                        |interface SingleTraitExtension extends BaseTrait""".stripMargin)
    }
    test("multiple class extension") {
      success("MultipleClassExtension#", """class BaseClass
                                         |interface BaseTrait
                                         |class MultipleClassExtension extends BaseClass, BaseTrait""".stripMargin)
    }
  }
}
