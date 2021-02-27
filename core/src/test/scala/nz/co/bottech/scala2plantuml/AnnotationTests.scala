package nz.co.bottech.scala2plantuml

import utest._

object AnnotationTests extends TestSuite with ClassDiagramTests {

  override protected val exampleDir: String = "annotation"

  val tests: Tests = Tests {
    test("annotation") {
      success("SimpleAnnotation#", """annotation SimpleAnnotation""")
    }
    test("extended annotation") {
      success("ExtendedAnnotation#", """annotation ExtendedAnnotation extends SimpleAnnotation
                                      |annotation SimpleAnnotation""".stripMargin)
    }
    test("static annotation") {
      success("MyStaticAnnotation#", """annotation MyStaticAnnotation""")
    }
    test("class annotation") {
      success("MyClassAnnotation#", """annotation MyClassAnnotation""")
    }
    test("trait annotation") {
      // Interface trumps annotation.
      success("TraitAnnotation#", """interface TraitAnnotation""")
    }
    test("case class annotation") {
      success("CaseClassAnnotation#", """annotation CaseClassAnnotation""")
    }
    test("object annotation") {
      success("ObjectAnnotation.", """annotation ObjectAnnotation""")
    }
    // TODO: Support Java types.
//    test("class annotation") {
//      success("MyJavaAnnotation", """annotation MyJavaAnnotation""")
//    }
  }
}
