package nz.co.bottech.scala2plantuml

import utest.{test, TestSuite, Tests}

object MaxLevelTests extends TestSuite with ClassDiagramTests {

  override protected val exampleDir: String = "maxlevel"

  val tests: Tests = Tests {
    test("maxlevel one field") {
      success(
        "FieldA#",
        """interface FieldA {
          |  + {abstract} {method} b
          |}""".stripMargin,
        maxLevel = Some(1)
      )
    }
    test("maxlevel three fields") {
      success(
        "FieldA#",
        """interface FieldA {
          |  + {abstract} {method} b
          |}
          |FieldA o-- FieldB
          |interface FieldB {
          |  + {abstract} {method} c
          |}
          |FieldB o-- FieldC
          |interface FieldC {
          |  + {abstract} {method} d
          |}""".stripMargin,
        maxLevel = Some(3)
      )
    }
    test("maxlevel one parent") {
      success(
        "ExtendsA#",
        """interface ExtendsA""".stripMargin,
        maxLevel = Some(1)
      )
    }
    test("maxlevel three parents") {
      success(
        "ExtendsA#",
        """interface ExtendsA extends ExtendsB
          |interface ExtendsB extends ExtendsC
          |interface ExtendsC""".stripMargin,
        maxLevel = Some(3)
      )
    }
  }
}
