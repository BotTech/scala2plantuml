package nz.co.bottech.scala2plantuml

import utest.{test, TestSuite, Tests}

object EnumerationTests extends TestSuite with ClassDiagramTests {

  override protected val exampleDir: String = "enumeration"

  val tests: Tests = Tests {
    test("enum") {
      success(
        "WeekDay.",
        """enum WeekDay {
          |  + {field} Fri
          |  + {field} Mon
          |  + {field} Sat
          |  + {field} Sun
          |  + {field} Thu
          |  + {field} Tue
          |  + {field} Wed
          |}""".stripMargin
      )
    }
    // TODO: Java enums (java.lang.Enum)
  }
}
