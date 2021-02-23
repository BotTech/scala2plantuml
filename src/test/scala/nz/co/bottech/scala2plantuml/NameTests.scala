package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramPrinter.Options
import utest.{test, TestSuite, Tests}

object NameTests extends TestSuite with ClassDiagramTest {

  override protected val exampleDir: String = "name"

  val tests: Tests = Tests {
    test("alphanumeric") {
      success("AlphaNumericName", """class Foo123""")
    }
    test("underscore") {
      success("NameWithUnderscore", """class Foo_123""")
    }
    test("hyphen") {
      success("NameWithHyphen", """class Foo-123""")
    }
    test("space") {
      success("NameWithSpace", """class "Foo 123"""")
    }
    test("symbols") {
      success("NameWithSymbols", """class "Foo!@#$123"""")
    }
    test("fully qualified") {
      success(
        "AlphaNumericName",
        """class nz.co.bottech.scala2plantuml.examples.name.Foo123""",
        Options.default.copy(namingStrategy = Options.FullyQualified)
      )
    }
  }
}

/**
 * @startuml
 * class nz.co.bottech.scala2plantuml.Foo123
 * @enduml
 */
