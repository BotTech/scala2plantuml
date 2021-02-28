package nz.co.bottech.scala2plantuml

import utest._

object NaturalTypeOrderingTests extends TestSuite {

  val tests: Tests = Tests {
    test("type before member") {
      assert(NaturalTypeOrdering.compare("Foo#", "Foo#a") < 0)
    }
    test("numbered after non-numbered") {
      assert(NaturalTypeOrdering.compare("Foo#", "Foo2#") < 0)
    }
    test("member before nested type if earlier") {
      assert(NaturalTypeOrdering.compare("Foo#a.", "Foo#Bar#") < 0)
    }
    test("member before nested type if same") {
      assert(NaturalTypeOrdering.compare("Foo#bar.", "Foo#Bar#") < 0)
    }
    test("member before nested type if later") {
      assert(NaturalTypeOrdering.compare("Foo#c.", "Foo#Bar#") < 0)
    }
    test("member before double nested type if earlier") {
      assert(NaturalTypeOrdering.compare("Foo#a.", "Foo#Bar#Baz#") < 0)
    }
    test("member before double nested type if same") {
      assert(NaturalTypeOrdering.compare("Foo#barbaz.", "Foo#Bar#Baz#") < 0)
    }
    test("member before double nested type if later") {
      assert(NaturalTypeOrdering.compare("Foo#c.", "Foo#Bar#Baz#") < 0)
    }
    test("nested before unrelated if earlier") {
      assert(NaturalTypeOrdering.compare("Foo#Bar#", "Goo#a.") < 0)
    }
    test("nested after unrelated if later") {
      assert(NaturalTypeOrdering.compare("Baz#a.", "Foo#Bar#") < 0)
    }
    test("class before object if earlier") {
      assert(NaturalTypeOrdering.compare("Bar#", "Foo.") < 0)
    }
    test("shorter type before longer type") {
      assert(NaturalTypeOrdering.compare("Foo#bar2.", "Foobar#") < 0)
    }
  }
}
