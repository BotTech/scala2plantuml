package nz.co.bottech.scala2plantuml

import utest.{test, TestSuite, Tests}

object FieldTests extends TestSuite with ClassDiagramTests {

  override protected val exampleDir: String = "field"

  val tests: Tests = Tests {
    test("public field") {
      success("PublicField#", """class PublicField {
                               |  + {field} field
                               |}""".stripMargin)
    }
    test("private field") {
      success("PrivateField#", """class PrivateField {
                                |  - {field} field
                                |}""".stripMargin)
    }
    test("private this field") {
      success(
        "PrivateThisField#",
        """class PrivateThisField {
          |  - {field} field
          |}""".stripMargin
      )
    }
    test("private package field") {
      success(
        "PrivatePackageField#",
        """class PrivatePackageField {
          |  ~ {field} field
          |}""".stripMargin
      )
    }
    test("protected field") {
      success("ProtectedField#", """class ProtectedField {
                                  |  # {field} field
                                  |}""".stripMargin)
    }
    test("protected this field") {
      success(
        "ProtectedThisField#",
        """class ProtectedThisField {
          |  # {field} field
          |}""".stripMargin
      )
    }
    test("protected package field") {
      success(
        "ProtectedPackageField#",
        """class ProtectedPackageField {
          |  # {field} field
          |}""".stripMargin
      )
    }
  }
}
