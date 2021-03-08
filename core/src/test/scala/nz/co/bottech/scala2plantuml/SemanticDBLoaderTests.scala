package nz.co.bottech.scala2plantuml

import utest._

object SemanticDBLoaderTests extends TestSuite {

  val tests: Tests = Tests {
    test("class") {
      test("field") {
        val path = SemanticDBLoader.semanticdbPath("a/b/C#d.")
        assert(Right("a/b/C.scala.semanticdb") == path)
      }
      test("method") {
        val path = SemanticDBLoader.semanticdbPath("a/b/C#d().")
        assert(Right("a/b/C.scala.semanticdb") == path)
      }
      test("method default") {
        val path = SemanticDBLoader.semanticdbPath("a/b/C#d$default$1().")
        assert(Right("a/b/C.scala.semanticdb") == path)
      }
      test("method overload") {
        val path = SemanticDBLoader.semanticdbPath("a/b/C#d(+1).")
        assert(Right("a/b/C.scala.semanticdb") == path)
      }
      test("method setter") {
        val path = SemanticDBLoader.semanticdbPath("a/b/C#d_=().")
        assert(Right("a/b/C.scala.semanticdb") == path)
      }
      test("constructor") {
        val path = SemanticDBLoader.semanticdbPath("a/b/C#`<init>`().")
        assert(Right("a/b/C.scala.semanticdb") == path)
      }
      test("type member") {
        val path = SemanticDBLoader.semanticdbPath("a/b/C#D#")
        assert(Right("a/b/C.scala.semanticdb") == path)
      }
      test("inner object") {
        val path = SemanticDBLoader.semanticdbPath("a/b/C#D.")
        assert(Right("a/b/C.scala.semanticdb") == path)
      }
    }
    test("object") {
      test("field") {
        val path = SemanticDBLoader.semanticdbPath("a/b/C.d.")
        assert(Right("a/b/C.scala.semanticdb") == path)
      }
      test("method") {
        val path = SemanticDBLoader.semanticdbPath("a/b/C.d().")
        assert(Right("a/b/C.scala.semanticdb") == path)
      }
      test("method default") {
        val path = SemanticDBLoader.semanticdbPath("a/b/C.d$default$1().")
        assert(Right("a/b/C.scala.semanticdb") == path)
      }
      test("method overload") {
        val path = SemanticDBLoader.semanticdbPath("a/b/C.d(+1).")
        assert(Right("a/b/C.scala.semanticdb") == path)
      }
      test("method setter") {
        val path = SemanticDBLoader.semanticdbPath("a/b/C.d_=().")
        assert(Right("a/b/C.scala.semanticdb") == path)
      }
      test("constructor") {
        val path = SemanticDBLoader.semanticdbPath("a/b/C.`<init>`().")
        assert(Right("a/b/C.scala.semanticdb") == path)
      }
      test("type member") {
        val path = SemanticDBLoader.semanticdbPath("a/b/C.D#")
        assert(Right("a/b/C.scala.semanticdb") == path)
      }
      test("inner object") {
        val path = SemanticDBLoader.semanticdbPath("a/b/C.D.")
        assert(Right("a/b/C.scala.semanticdb") == path)
      }
    }
    test("package object") {
      test("method") {
        val path = SemanticDBLoader.semanticdbPath("a/b/c/package.d().")
        assert(Right("a/b/c.scala.semanticdb") == path)
      }
      test("method default") {
        val path = SemanticDBLoader.semanticdbPath("a/b/c/package.d$default$1().")
        assert(Right("a/b/c.scala.semanticdb") == path)
      }
      test("method overload") {
        val path = SemanticDBLoader.semanticdbPath("a/b/c/package.d(+1).")
        assert(Right("a/b/c.scala.semanticdb") == path)
      }
      test("method setter") {
        val path = SemanticDBLoader.semanticdbPath("a/b/c/package.d_=().")
        assert(Right("a/b/c.scala.semanticdb") == path)
      }
      test("constructor") {
        val path = SemanticDBLoader.semanticdbPath("a/b/c/package.`<init>`().")
        assert(Right("a/b/c.scala.semanticdb") == path)
      }
      test("type member") {
        val path = SemanticDBLoader.semanticdbPath("a/b/c/package.D#")
        assert(Right("a/b/c.scala.semanticdb") == path)
      }
      test("inner object") {
        val path = SemanticDBLoader.semanticdbPath("a/b/c/package.D.")
        assert(Right("a/b/c.scala.semanticdb") == path)
      }
    }
  }
}
