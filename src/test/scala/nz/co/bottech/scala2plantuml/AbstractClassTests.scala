package nz.co.bottech.scala2plantuml

import utest.{TestSuite, Tests, assert, test}

object AbstractClassTests extends TestSuite {

  val tests: Tests = Tests {
    test("abstract class") {
      val diagram = ClassDiagramGenerator.generate("""abstract class Foo""")
      assert(diagram == Right("""abstract class Foo"""))
    }
    // TODO: Add tests with other modifiers
  }
}
