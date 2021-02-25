package nz.co.bottech.scala2plantuml

import utest.{test, TestSuite, Tests}

object AbstractClassTests extends TestSuite with ClassDiagramTest {

  override protected val exampleDir: String = "abstractclass"

  val tests: Tests = Tests {
    test("abstract class") {
      success("AbstractClass#", """abstract class AbstractClass""")
    }
    test("sealed abstract class") {
      success("SealedAbstractClass#", """abstract class SealedAbstractClass""")
    }
  }
}
