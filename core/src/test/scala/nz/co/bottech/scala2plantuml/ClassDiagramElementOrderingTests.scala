package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramElement._
import utest._

object ClassDiagramElementOrderingTests extends TestSuite {

  private val lexicographic = new ClassDiagramElementOrdering((x: String, y: String) => x.compare(y))

  private val typA                = typ("a#")
  private val typB                = typ("b#")
  private val typC                = typ("c#")
  private def typ(symbol: String) = Class("", symbol, isObject = false, isAbstract = false, Seq.empty, Seq.empty)

  private val typATypC = typ(s"${typA.symbol}${typC.symbol}")
  private val typBTypA = typ(s"${typB.symbol}${typA.symbol}")
  private val typBTypB = typ(s"${typB.symbol}${typB.symbol}")
  private val typBTypC = typ(s"${typB.symbol}${typC.symbol}")
  private val typCTypA = typ(s"${typC.symbol}${typA.symbol}")

  private val typAMemberC = typMember(s"${typA.symbol}c.")
  private val typBMemberA = typMember(s"${typB.symbol}a.")
  private val typBMemberB = typMember(s"${typB.symbol}b.")
  private val typBMemberC = typMember(s"${typB.symbol}c.")
  private val typCMemberA = typMember(s"${typC.symbol}a.")

  private val typBTypBMemberB = typMember(s"${typBTypB.symbol}b.")

  private def typMember(symbol: String) =
    Method("", symbol, Visibility.Public, constructor = false, synthetic = false, isAbstract = false, Seq.empty)

  private val typAToC = Aggregation(typA.symbol, typC.symbol)
  private val typBToA = Aggregation(typB.symbol, typA.symbol)
  private val typBToB = Aggregation(typB.symbol, typB.symbol)
  private val typBToC = Aggregation(typB.symbol, typC.symbol)
  private val typCToA = Aggregation(typC.symbol, typA.symbol)

  private val exampleClass = Class(
    "FieldRelation",
    "nz/co/bottech/scala2plantuml/examples/field/FieldRelation#",
    isObject = false,
    isAbstract = false,
    Seq.empty,
    Seq.empty
  )

  private val exampleField = Field(
    "field",
    "nz/co/bottech/scala2plantuml/examples/field/FieldRelation#field.",
    Visibility.Public,
    isAbstract = false
  )

  private val exampleInterface =
    Interface("Trait", "nz/co/bottech/scala2plantuml/examples/field/Trait#", Seq.empty, Seq.empty)

  val tests: Tests = Tests {
    test("type") {
      test("and different type") {
        test("before when earlier") {
          assert(lexicographic.compare(typA, typB) < 0)
        }
      }
      test("and member of same type") {
        test("before when earlier") {
          assert(lexicographic.compare(typB, typBMemberC) < 0)
        }
        test("before when same") {
          assert(lexicographic.compare(typB, typBMemberB) < 0)
        }
        test("before when later") {
          assert(lexicographic.compare(typB, typBMemberA) < 0)
        }
      }
      test("and member of different type") {
        test("before when earlier") {
          assert(lexicographic.compare(typB, typCMemberA) < 0)
        }
        test("after when later") {
          assert(lexicographic.compare(typB, typAMemberC) > 0)
        }
      }
      test("and aggregation") {
        test("before when earlier") {
          assert(lexicographic.compare(typB, typCToA) < 0)
        }
        test("before when same") {
          assert(lexicographic.compare(typB, typBToA) < 0)
        }
        test("after when later") {
          assert(lexicographic.compare(typB, typAToC) > 0)
        }
      }
    }
    test("inner type") {
      test("and same parent type") {
        test("after when earlier") {
          assert(lexicographic.compare(typBTypA, typB) > 0)
        }
        test("after when same") {
          assert(lexicographic.compare(typBTypB, typB) > 0)
        }
        test("after when later") {
          assert(lexicographic.compare(typBTypC, typB) > 0)
        }
      }
      test("and different parent type") {
        test("before when earlier") {
          assert(lexicographic.compare(typBTypB, typC) < 0)
        }
        test("after when later") {
          assert(lexicographic.compare(typBTypB, typA) > 0)
        }
      }
      test("and same parent inner type") {
        test("before when earlier") {
          assert(lexicographic.compare(typBTypA, typBTypC) < 0)
        }
        test("after when later") {
          assert(lexicographic.compare(typBTypC, typBTypA) > 0)
        }
      }
      test("and different parent inner type") {
        test("before when earlier") {
          assert(lexicographic.compare(typBTypB, typCTypA) < 0)
        }
        test("after when later") {
          assert(lexicographic.compare(typBTypB, typATypC) > 0)
        }
      }
      test("and member of parent type") {
        test("after when earlier") {
          assert(lexicographic.compare(typBTypA, typBMemberC) > 0)
        }
        test("after when same") {
          assert(lexicographic.compare(typBTypB, typBMemberB) > 0)
        }
        test("after when later") {
          assert(lexicographic.compare(typBTypC, typBMemberA) > 0)
        }
      }
      test("and aggregation") {
        test("before when earlier") {
          assert(lexicographic.compare(typBTypC, typCToA) < 0)
        }
        test("after when same") {
          assert(lexicographic.compare(typBTypC, typBToA) > 0)
        }
        test("after when later") {
          assert(lexicographic.compare(typBTypA, typAToC) > 0)
        }
      }
    }
    test("member") {
      test("and member of same parent type") {
        test("before when earlier") {
          assert(lexicographic.compare(typBMemberB, typBMemberC) < 0)
        }
        test("after when later") {
          assert(lexicographic.compare(typBMemberB, typBMemberA) > 0)
        }
      }
      test("and member of different parent type") {
        test("before when earlier") {
          assert(lexicographic.compare(typBMemberB, typCMemberA) < 0)
        }
        test("after when later") {
          assert(lexicographic.compare(typBMemberB, typAMemberC) > 0)
        }
      }
      test("and aggregation") {
        test("before when earlier") {
          assert(lexicographic.compare(typBMemberB, typCToA) < 0)
        }
        test("before when same") {
          assert(lexicographic.compare(typBMemberB, typBToA) < 0)
        }
        test("after when later") {
          assert(lexicographic.compare(typBMemberB, typAToC) > 0)
        }
      }
    }
    test("inner member") {
      test("and member of same parent type") {
        test("after when earlier") {
          assert(lexicographic.compare(typBTypBMemberB, typBMemberC) > 0)
        }
        test("after when same") {
          assert(lexicographic.compare(typBTypBMemberB, typBMemberB) > 0)
        }
        test("after when later") {
          assert(lexicographic.compare(typBTypBMemberB, typBMemberA) > 0)
        }
      }
      test("and member of different parent type") {
        test("before when earlier") {
          assert(lexicographic.compare(typBTypBMemberB, typCMemberA) < 0)
        }
        test("after when later") {
          assert(lexicographic.compare(typBTypBMemberB, typAMemberC) > 0)
        }
      }
      test("and aggregation") {
        test("after when earlier") {
          assert(lexicographic.compare(typBTypBMemberB, typBToC) > 0)
        }
        test("after when same") {
          assert(lexicographic.compare(typBTypBMemberB, typBToB) > 0)
        }
        test("after when later") {
          assert(lexicographic.compare(typBTypBMemberB, typBToA) > 0)
        }
      }
    }
    test("aggregation") {
      test("and aggregation from same") {
        test("before when earlier") {
          assert(lexicographic.compare(typBToB, typBToC) < 0)
        }
        test("after when later") {
          assert(lexicographic.compare(typBToB, typBToA) > 0)
        }
      }
      test("and aggregation from different") {
        test("before when earlier") {
          assert(lexicographic.compare(typBToB, typCToA) < 0)
        }
        test("after when later") {
          assert(lexicographic.compare(typBToB, typAToC) > 0)
        }
      }
    }
    test("examples") {
      test("field relation") {
        test("exampleInterface vs exampleClass") {
          assert(lexicographic.compare(exampleInterface, exampleClass) > 0)
        }
        test("exampleInterface vs exampleField") {
          assert(lexicographic.compare(exampleInterface, exampleField) > 0)
        }
        test("exampleClass vs exampleField") {
          assert(lexicographic.compare(exampleClass, exampleField) < 0)
        }
      }
    }
  }
}
