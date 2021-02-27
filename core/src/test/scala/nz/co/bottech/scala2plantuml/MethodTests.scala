package nz.co.bottech.scala2plantuml

import utest.{test, TestSuite, Tests}

object MethodTests extends TestSuite with ClassDiagramTests {

  override protected val exampleDir: String = "method"

  val tests: Tests = Tests {
    test("public method") {
      success("PublicMethod#", """class PublicMethod {
                                |  + {method} method
                                |}""".stripMargin)
    }
    test("private method") {
      success("PrivateMethod#", """class PrivateMethod {
                                 |  - {method} method
                                 |}""".stripMargin)
    }
    test("private this method") {
      success(
        "PrivateThisMethod#",
        """class PrivateThisMethod {
          |  - {method} method
          |}""".stripMargin
      )
    }
    test("private package method") {
      success(
        "PrivatePackageMethod#",
        """class PrivatePackageMethod {
          |  ~ {method} method
          |}""".stripMargin
      )
    }
    test("protected method") {
      success(
        "ProtectedMethod#",
        """class ProtectedMethod {
          |  # {method} method
          |}""".stripMargin
      )
    }
    test("protected this method") {
      success(
        "ProtectedThisMethod#",
        """class ProtectedThisMethod {
          |  # {method} method
          |}""".stripMargin
      )
    }
    test("protected package method") {
      success(
        "ProtectedPackageMethod#",
        """class ProtectedPackageMethod {
          |  # {method} method
          |}""".stripMargin
      )
    }
    test("method with bad file name") {
      success(
        "IdentityCrisis#",
        """class IdentityCrisis {
          |  + {method} method
          |}""".stripMargin
      )
    }
  }
}
