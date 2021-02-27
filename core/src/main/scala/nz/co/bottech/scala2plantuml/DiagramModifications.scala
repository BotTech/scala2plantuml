package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramElement._
import nz.co.bottech.scala2plantuml.ClassDiagramPrinter.Options.Unsorted
import nz.co.bottech.scala2plantuml.ClassDiagramPrinter._

import scala.annotation.tailrec
import scala.meta.internal.semanticdb.Scala._

private[scala2plantuml] object DiagramModifications {

  final case class ElementsWithNames(elements: Seq[ClassDiagramElement], names: Map[String, String])

  implicit class FluidOptions(val elementsWithNames: ElementsWithNames) extends AnyVal {

    import elementsWithNames._

    def removeHidden(implicit options: Options): ElementsWithNames =
      options.hide match {
        case Options.HideMatching(patterns) =>
          val tests                         = patterns.map(patternToRegex(_).asMatchPredicate)
          def hide(symbol: String): Boolean = tests.exists(_.test(symbol))
          val newElements = elements.filterNot(element => hide(element.symbol)).map {
            case typ: Type =>
              val newParents = typ.parentSymbols.filterNot(hide)
              typ match {
                case annotation: Annotation => annotation.copy(parentSymbols = newParents)
                case clazz: Class           => clazz.copy(parentSymbols = newParents)
                case enum: Enum             => enum.copy(parentSymbols = newParents)
                case interface: Interface   => interface.copy(parentSymbols = newParents)
              }
            case element => element
          }
          elementsWithNames.copy(elements = newElements)
        case Options.ShowAll => elementsWithNames
      }

    def removeSynthetics(implicit options: Options): ElementsWithNames =
      options.syntheticMethods match {
        case Options.HideSyntheticMethods =>
          val newElements = elements.filterNot {
            case method: Method => !method.constructor && method.synthetic
            case _              => false
          }
          elementsWithNames.copy(elements = newElements)
        case Options.ShowSyntheticMethods => elementsWithNames
      }

    def addMissingElements(implicit options: Options): ElementsWithNames =
      options.sorting match {
        case Unsorted =>
          // Fields may appear before their parents so we may need to insert duplicate parents and we have to do that
          // before other operations like renaming.
          val types = elements.filter(_.symbol.isType).map(element => element.symbol -> element).toMap
          @tailrec
          def loop(
              remaining: Seq[ClassDiagramElement],
              previousType: Option[ClassDiagramElement],
              acc: Vector[ClassDiagramElement]
            ): Vector[ClassDiagramElement] =
            remaining match {
              case head +: tail =>
                if (head.isType) {
                  val nextAcc = if (previousType.exists(_.symbol == head.symbol)) acc else acc :+ head
                  loop(tail, Some(head), nextAcc)
                } else if (head.isMember) {
                  val owner = head.ownerSymbol
                  if (previousType.exists(_.symbol == owner)) loop(tail, previousType, acc :+ head)
                  else {
                    // TODO: Do we need to caching this?
                    def createClass =
                      Class(
                        scalaTypeName(symbolToScalaIdentifier(owner)),
                        owner,
                        isObject = false,
                        isAbstract = false,
                        Seq.empty
                      )
                    val ownerElement = types.getOrElse(owner, createClass)
                    loop(tail, Some(ownerElement), acc :+ ownerElement :+ head)
                  }
                } else
                  loop(tail, previousType, acc :+ head)
              case _ => acc
            }
          val newElements = loop(elements, None, Vector.empty)
          elementsWithNames.copy(elements = newElements)
        case _ => elementsWithNames
      }

    def calculateNames(implicit options: Options): ElementsWithNames = {
      assert(names.isEmpty)
      val newNames = options.naming match {
        case Options.FullyQualified =>
          elements.map { element =>
            val displayName =
              if (element.isType) symbolToScalaIdentifier(element.symbol).replace("`", "")
              else element.displayName
            element.symbol -> displayName
          }.toMap
        case Options.RemoveCommonPrefix =>
          // FIXME: This will probably break links to types in other packages.
          val firstPrefix = elements.headOption.map { element =>
            val i = element.symbol.lastIndexOf('/')
            element.symbol.take(i + 1)
          }.getOrElse("")
          val commonPrefix = elements.foldLeft(firstPrefix) {
            case (prefix, element) => longestPrefix(prefix, element.symbol)
          }
          elements.map { element =>
            val displayName =
              if (element.isType) symbolToScalaIdentifier(element.symbol.drop(commonPrefix.length)).replace("`", "")
              else element.displayName
            element.symbol -> displayName
          }.toMap
      }
      elementsWithNames.copy(names = newNames)
    }

    def combineCompanionObjects(implicit options: Options): ElementsWithNames =
      options.companionObjects match {
        case Options.CombineAsStatic =>
          val nonObjectNames =
            elements
              .filterNot(_.isObject)
              .map(element => symbolToScalaIdentifier(element.symbol))
              .toSet
          val newElements = elements.filterNot(element =>
            element.isObject && nonObjectNames.contains(symbolToScalaIdentifier(element.symbol))
          )
          elementsWithNames.copy(elements = newElements)
        case Options.SeparateClasses =>
          val newNames = elements.foldLeft(names) {
            case (names, element) if element.isObject =>
              val symbol = element.symbol
              val name   = names.getOrElse(symbol, element.displayName)
              names.updated(symbol, s"$name$$")
            case (names, _) => names
          }
          elementsWithNames.copy(names = newNames)
      }

    def updateConstructors(implicit options: Options): ElementsWithNames =
      options.constructor match {
        case Options.HideConstructors =>
          val newElements = elements.filterNot {
            case method: Method => method.constructor
            case _              => false
          }
          elementsWithNames.copy(elements = newElements)
        case Options.ShowConstructors(stereotype, name) =>
          val newNames = elements.foldLeft(names) {
            case (names, method: Method) if method.constructor =>
              val newName            = name(method)
              val nameWithStereotype = stereotype.map(s => s"<<$s>> $newName").getOrElse(newName)
              names.updated(method.symbol, nameWithStereotype)
            case (names, _) => names
          }
          elementsWithNames.copy(names = newNames)
      }

    def sort(implicit options: Options): ElementsWithNames = {
      val newElements = options.sorting match {
        case Options.NaturalSortOrder => elements.sortBy(_.symbol)(new NaturalOrdering)
        case Options.Unsorted         => elements
      }
      elementsWithNames.copy(elements = newElements)
    }

    private def longestPrefix(a: String, b: String): String = {
      val i = (0 until math.min(a.length, b.length)).takeWhile { i =>
        a(i) == b(i)
      }.lastOption.getOrElse(-1)
      a.take(i + 1)
    }
  }

  def apply(elements: Seq[ClassDiagramElement], options: Options): ElementsWithNames = {
    implicit val opts: Options = options
    ElementsWithNames(
      elements,
      Map.empty
    ).removeHidden.removeSynthetics.addMissingElements.calculateNames.combineCompanionObjects.updateConstructors.sort
  }
}
