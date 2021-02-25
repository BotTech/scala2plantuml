package nz.co.bottech.scala2plantuml

import utest.{test, TestSuite, Tests}

object ClassTests extends TestSuite with ClassDiagramTest {

  override protected val exampleDir: String = "clazz"

  val tests: Tests = Tests {
    test("simple class") {
      success("SimpleClass#", """class SimpleClass""")
    }
    test("case class") {
      success("CaseClass#", """class CaseClass""")
    }
    test("object class") {
      success("ObjectClass.", """class ObjectClass""")
    }
  }
}
