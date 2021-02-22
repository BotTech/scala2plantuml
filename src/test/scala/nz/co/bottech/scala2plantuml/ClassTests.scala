package nz.co.bottech.scala2plantuml

import utest.{TestSuite, Tests, test}

object ClassTests extends TestSuite with GeneratorTest {

  override protected val exampleDir: String = "clazz"

  val tests: Tests = Tests {
    test("simple class") {
      success("SimpleClass", """class SimpleClass""")
    }
    test("case class") {
      success("CaseClass", """class CaseClass""")
    }
    test("object class") {
      success("ObjectClass", """class ObjectClass""")
    }
  }
}
