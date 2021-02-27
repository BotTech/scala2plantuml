package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramElement._
import nz.co.bottech.scala2plantuml.ClassDiagramPrinter.Options.Unsorted
import nz.co.bottech.scala2plantuml.ClassDiagramPrinter._

import scala.annotation.tailrec
import scala.meta.internal.semanticdb.Scala._

private[scala2plantuml] object DiagramModifications {

  implicit class FluidOptions(val elements: Seq[ClassDiagramElement]) extends AnyVal {

    def removeHidden(implicit options: Options): Seq[ClassDiagramElement] =
      options.hide match {
        case Options.ShowAll => elements
        case Options.HideMatching(patterns) =>
          val tests = patterns.map(patternToRegex(_).asMatchPredicate)
          val result = elements.filterNot { element =>
            tests.exists(_.test(element.symbol))
          }
          result
      }

    def removeSynthetics(implicit options: Options): Seq[ClassDiagramElement] =
      options.syntheticMethods match {
        case Options.HideSyntheticMethods =>
          elements.filterNot {
            case method: Method => !method.constructor && method.synthetic
            case _              => false
          }
        case Options.ShowSyntheticMethods =>
          elements
      }

    def addMissingElements(implicit options: Options): Seq[ClassDiagramElement] =
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
                        isAbstract = false
                      )
                    val ownerElement = types.getOrElse(owner, createClass)
                    loop(tail, Some(ownerElement), acc :+ ownerElement :+ head)
                  }
                } else
                  loop(tail, previousType, acc :+ head)
              case _ => acc
            }
          loop(elements, None, Vector.empty)
        case _ => elements
      }

    def combineCompanionObjects(implicit options: Options): Seq[ClassDiagramElement] =
      options.companionObjects match {
        case Options.CombineAsStatic =>
          val nonObjectNames =
            elements
              .filterNot(_.isObject)
              .map(element => symbolToScalaIdentifier(element.symbol))
              .toSet
          elements.filterNot(element =>
            element.isObject && nonObjectNames.contains(symbolToScalaIdentifier(element.symbol))
          )
        case Options.SeparateClasses =>
          elements.map { element =>
            if (element.isObject) element.rename(s"${element.displayName}$$")
            else element
          }
      }

    def rename(implicit options: Options): Seq[ClassDiagramElement] =
      options.naming match {
        case Options.FullyQualified =>
          elements.map { element =>
            val displayName =
              if (element.isType) symbolToScalaIdentifier(element.symbol).replace("`", "")
              else element.displayName
            element.rename(displayName)
          }
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
            element.rename(displayName)
          }
      }

    def updateConstructors(implicit options: Options): Seq[ClassDiagramElement] =
      options.constructor match {
        case Options.HideConstructors =>
          elements.filterNot {
            case method: Method => method.constructor
            case _              => false
          }
        case Options.ShowConstructors(stereotype, name) =>
          elements.map {
            case method: Method if method.constructor =>
              val newName            = name(method)
              val nameWithStereotype = stereotype.map(s => s"<<$s>> $newName").getOrElse(newName)
              method.copy(displayName = nameWithStereotype)
            case element => element
          }
      }

    def sort(implicit options: Options): Seq[ClassDiagramElement] =
      options.sorting match {
        case Options.NaturalSortOrder => elements.sortBy(_.symbol)(new NaturalOrdering)
        case Options.Unsorted         => elements
      }

    private def longestPrefix(a: String, b: String): String = {
      val i = (0 until math.min(a.length, b.length)).takeWhile { i =>
        a(i) == b(i)
      }.lastOption.getOrElse(-1)
      a.take(i + 1)
    }
  }

  def apply(elements: Seq[ClassDiagramElement], options: Options): Seq[ClassDiagramElement] = {
    implicit val opts: Options = options
    elements.removeHidden.removeSynthetics.addMissingElements.combineCompanionObjects.rename.updateConstructors.sort
  }
}
