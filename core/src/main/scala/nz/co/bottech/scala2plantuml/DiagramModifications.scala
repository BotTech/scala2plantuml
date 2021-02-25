package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramElement._
import nz.co.bottech.scala2plantuml.ClassDiagramPrinter.Options.Unsorted
import nz.co.bottech.scala2plantuml.ClassDiagramPrinter._

import java.util.regex.Pattern
import scala.meta.internal.semanticdb.Scala._
import scala.util.matching.Regex

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

    def combineCompanionObjects(implicit options: Options): Seq[ClassDiagramElement] =
      options.companionObjects match {
        case Options.CombineAsStatic =>
          val nonObjectNames =
            elements
              .filterNot(ClassDiagramElement.isObject)
              .map(element => symbolToScalaIdentifier(element.symbol))
              .toSet
          elements.filterNot(element =>
            ClassDiagramElement.isObject(element) && nonObjectNames.contains(symbolToScalaIdentifier(element.symbol))
          )
        case Options.SeparateClasses =>
          elements.map { element =>
            if (ClassDiagramElement.isObject(element)) renameElement(element, _.displayName + "$")
            else element
          }
      }

    def rename(implicit options: Options): Seq[ClassDiagramElement] =
      options.naming match {
        case Options.FullyQualified =>
          elements.map(renameElement(_, element => symbolToScalaIdentifier(element.symbol).replace("`", "")))
        case Options.RemoveCommonPrefix =>
          // FIXME: This will probably break links to types in other packages.
          val firstPrefix = elements.headOption.map { element =>
            val i = element.symbol.lastIndexOf('/')
            element.symbol.take(i + 1)
          }.getOrElse("")
          val commonPrefix = elements.foldLeft(firstPrefix) {
            case (prefix, element) => longestPrefix(prefix, element.symbol)
          }
          elements.map(
            renameElement(
              _,
              element => symbolToScalaIdentifier(element.symbol.drop(commonPrefix.length)).replace("`", "")
            )
          )
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

    def addMissingElements(implicit options: Options): Seq[ClassDiagramElement] =
      options.sorting match {
        case Unsorted =>
          // Fields may appear before their parents so we may need to insert duplicate parents and we have to do that
          // before other operations like renaming.
          val types = elements.filter(_.symbol.isType).map(element => element.symbol -> element).toMap
          val (_, withMissing) = elements.foldLeft((List.empty[String], Vector.empty[ClassDiagramElement])) {
            case ((outer, acc), element) =>
              if (isMember(element)) {
                val parent       = methodParent(element.symbol)
                def createParent = Class(scalaTypeName(symbolToScalaIdentifier(parent)), parent, isObject = false)
                outer match {
                  case head :: tail =>
                    if (head == parent)
                      (outer, acc :+ element)
                    else {
                      val clazz = types.getOrElse(parent, createParent)
                      (parent +: tail, acc :+ clazz :+ element)
                    }
                  case Nil =>
                    val clazz = types.getOrElse(parent, createParent)
                    (List(parent), acc :+ clazz :+ element)
                }
              } else
                (outer, acc :+ element)
          }
          withMissing
        case _ => elements
      }

    def sort(implicit options: Options): Seq[ClassDiagramElement] =
      options.sorting match {
        case Options.NaturalSortOrder => elements.sortBy(_.symbol)(new NaturalOrdering)
        case Options.Unsorted         => elements
      }

    private def patternToRegex(pattern: String): Pattern =
      Pattern.compile(
        pattern
          .split("""\*\*""", -1)
          .map(_.split("""\*""", -1).map(Regex.quote).mkString("""[^/]*"""))
          .mkString(""".*""")
      )

    private def longestPrefix(a: String, b: String): String = {
      val i = (0 until math.min(a.length, b.length)).takeWhile { i =>
        a(i) == b(i)
      }.lastOption.getOrElse(-1)
      a.take(i + 1)
    }

    private def renameElement(element: ClassDiagramElement, f: ClassDiagramElement => String): ClassDiagramElement =
      element match {
        case element: AbstractClass => element.copy(displayName = f(element))
        case element: Annotation    => element.copy(displayName = f(element))
        case element: Enum          => element.copy(displayName = f(element))
        case element: Field         => element
        case element: Class         => element.copy(displayName = f(element))
        case element: Interface     => element.copy(displayName = f(element))
        case element: Method        => element
      }

    private def isMember(element: ClassDiagramElement): Boolean =
      element match {
        case _: Field  => true
        case _: Method => true
        case _         => false
      }
  }

  def apply(elements: Seq[ClassDiagramElement], options: Options): Seq[ClassDiagramElement] = {
    implicit val opts: Options = options
    elements.removeHidden.removeSynthetics.addMissingElements.combineCompanionObjects.rename.updateConstructors.sort
  }
}
