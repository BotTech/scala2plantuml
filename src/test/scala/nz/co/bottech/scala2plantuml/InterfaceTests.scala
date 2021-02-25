package nz.co.bottech.scala2plantuml

import utest.{test, TestSuite, Tests}

object InterfaceTests extends TestSuite with ClassDiagramTest {

  override protected val exampleDir: String = "interfaces"

  val tests: Tests = Tests {
    test("interface") {
      success("MyTrait#", """interface MyTrait""")
    }
  }
}
