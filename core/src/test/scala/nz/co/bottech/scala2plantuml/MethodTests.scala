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
    test("method return type") {
      success(
        "MethodReturnType#",
        """class MethodReturnType {
          |  + {method} method
          |}
          |MethodReturnType o-- Trait
          |interface Trait""".stripMargin
      )
    }
    test("method returns self") {
      success(
        "MethodReturnsSelf#",
        """class MethodReturnsSelf {
          |  + {method} method
          |}""".stripMargin
      )
    }
    test("method parameter") {
      success(
        "MethodParameter#",
        """class MethodParameter {
          |  + {method} method
          |}
          |MethodParameter o-- Trait
          |interface Trait""".stripMargin
      )
    }
    test("higher-kinded method parameter") {
      success(
        "HigherKindedParameter#",
        """class HigherKindedParameter {
          |  + {method} method
          |}""".stripMargin
      )
    }
    test("higher-kinded method parameter filled") {
      success(
        "HigherKindedParameterFilled#",
        """class HigherKindedParameterFilled {
          |  + {method} method
          |}
          |HigherKindedParameterFilled o-- Trait
          |interface Trait""".stripMargin
      )
    }
  }
}
