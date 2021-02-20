package nz.co.bottech.scala2plantuml

import utest.{TestSuite, Tests, assert, test}

object DeclaringElementsTests extends TestSuite {
  val tests: Tests = Tests {
    test("abstract class") {
      val diagram = ClassDiagramGenerator.generate("""abstract class `abstract class`""")
      assert(diagram == Right("""abstract class "abstract class""""))
    }
  }
}
