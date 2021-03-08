package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramRenderer.Options
import utest.{test, TestSuite, Tests}

object NameTests extends TestSuite with ClassDiagramTests {

  override protected val exampleDir: String = "name"

  val tests: Tests = Tests {
    test("alphanumeric") {
      success("Foo123#", """class Foo123""")
    }
    test("underscore") {
      success("Foo_123#", """class Foo_123""")
    }
    test("hyphen") {
      success("`Foo-123`#", """class Foo-123""")
    }
    test("space") {
      success("`Foo 123`#", """class "Foo 123"""")
    }
    test("symbols") {
      success("`Foo!@#$123`#", """class "Foo!@#$123"""")
    }
    test("fully qualified") {
      success(
        "Foo123#",
        """class nz.co.bottech.scala2plantuml.examples.name.Foo123""",
        options = testOptions.copy(naming = Options.FullyQualified)
      )
    }
  }
}
