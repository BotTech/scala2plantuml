package nz.co.bottech.scala2plantuml

import utest.{assert, test, TestSuite, Tests}

import scala.util.{Success, Using}

object AbstractClassTests extends TestSuite {

  private def testSemanticdb(name: String) =
    s"META-INF/semanticdb/src/test/scala/nz/co/bottech/scala2plantuml/examples/abstractclass/$name.scala.semanticdb"

  val tests: Tests = Tests {
    test("class") {
      val path = testSemanticdb("ConcreteClass")
      val result = Using(getClass.getClassLoader.getResourceAsStream(path))(
        ClassDiagramGenerator.fromInputStream
      )
      assert(result == Success(Right("""class Foo""")))
    }
    test("abstract class") {
      val path = testSemanticdb("AbstractClass")
      val result = Using(getClass.getClassLoader.getResourceAsStream(path))(
        ClassDiagramGenerator.fromInputStream
      )
      assert(result == Success(Right("""abstract class Foo""")))
    }
    // TODO: Add tests with other modifiers
  }
}
