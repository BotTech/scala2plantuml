package nz.co.bottech.scala2plantuml

import utest.{test, TestSuite, Tests}

object EnumerationTests extends TestSuite with ClassDiagramTests {

  override protected val exampleDir: String = "enumeration"

  val tests: Tests = Tests {
    test("enum") {
      success(
        "WeekDay.",
        """enum WeekDay {
          |  + {static} {field} Fri
          |  + {static} {field} Mon
          |  + {static} {field} Sat
          |  + {static} {field} Sun
          |  + {static} {field} Thu
          |  + {static} {field} Tue
          |  + {static} {field} Wed
          |}""".stripMargin
      )
    }
  }
}
