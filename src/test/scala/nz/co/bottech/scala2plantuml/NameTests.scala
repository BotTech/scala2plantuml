package nz.co.bottech.scala2plantuml

import utest.{test, TestSuite, Tests}

object NameTests extends TestSuite with GeneratorTest {

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
  }
}
