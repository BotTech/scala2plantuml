package nz.co.bottech.scala2plantuml

import utest.{TestSuite, Tests, test}

object EnumerationTests extends TestSuite with GeneratorTest {

  override protected val exampleDir: String = "enumeration"

  val tests: Tests = Tests {
    test("enum") {
      success("WeekDay", """enum WeekDay""")
    }
    // TODO: Java enums (java.lang.Enum)
  }
}
