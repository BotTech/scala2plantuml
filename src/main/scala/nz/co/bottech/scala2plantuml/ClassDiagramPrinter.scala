package nz.co.bottech.scala2plantuml

import scala.annotation.tailrec

object ClassDiagramPrinter {

  private val SomewhatSensibleName = """[\p{Alnum}._-]+""".r

  final case class Options(
      namingStrategy: Options.NamingStrategy,
      combineCompanionObjects: Boolean)

  object Options {

    val default: Options = Options(
      namingStrategy = RemoveCommonPrefix,
      combineCompanionObjects = true
    )

    sealed trait NamingStrategy
    case object FullyQualified     extends NamingStrategy
    case object RemoveCommonPrefix extends NamingStrategy
  }

  def print(elements: Seq[ClassDiagramElement], options: Options): String =
    s"""@startuml
       |${printSnippet(elements, options)}
       |@enduml""".stripMargin

  def printSnippet(elements: Seq[ClassDiagramElement], options: Options): String = {
    @tailrec
    def loop(remaining: Seq[ClassDiagramElement], acc: StringBuilder): String =
      remaining match {
        case head +: tail =>
          acc.append('\n')
          acc.append(printElement(head))
          loop(tail, acc)
        case Seq() => acc.toString
      }
    val updatedElements = applyOptions(elements, options)
    val builder         = new StringBuilder
    updatedElements.headOption.map(element => builder.append(printElement(element)))
    loop(updatedElements.drop(1), builder)
  }

  implicit private class FluidOptions(val elements: Seq[ClassDiagramElement]) extends AnyVal {

    def combineCompanionObjects(options: Options): Seq[ClassDiagramElement] =
      if (options.combineCompanionObjects) {
        val nonObjectNames = elements.filterNot(_.isObject).map(_.fullName).toSet
        elements.filterNot(element => element.isObject && nonObjectNames.contains(element.fullName))
      } else
        elements

    def rename(options: Options): Seq[ClassDiagramElement] =
      options.namingStrategy match {
        case Options.FullyQualified =>
          elements.map(renameElement(_, _.fullName.replace("`", "")))
        case Options.RemoveCommonPrefix =>
          val firstPrefix = elements.headOption.map { element =>
            val i = element.fullName.lastIndexOf('.')
            element.fullName.take(i + 1)
          }.getOrElse("")
          val commonPrefix = elements.foldLeft(firstPrefix) {
            case (prefix, element) => longestPrefix(prefix, element.fullName)
          }
          elements.map(renameElement(_, _.fullName.drop(commonPrefix.length).replace("`", "")))
      }

    private def longestPrefix(a: String, b: String): String = {
      val i = (0 until math.min(a.length, b.length)).findLast { i =>
        a(i) == b(i)
      }.getOrElse(-1)
      a.take(i + 1)
    }

    private def renameElement(element: ClassDiagramElement, f: ClassDiagramElement => String): ClassDiagramElement = {
      element match {
        case element: UmlAbstractClass => element.copy(displayName = f(element))
        case element: UmlAnnotation    => element.copy(displayName = f(element))
        case element: UmlEnum          => element.copy(displayName = f(element))
        case element: UmlClass         => element.copy(displayName = f(element))
        case element: UmlInterface     => element.copy(displayName = f(element))
      }
    }
  }

  private def applyOptions(elements: Seq[ClassDiagramElement], options: Options): Seq[ClassDiagramElement] =
    elements
      .combineCompanionObjects(options)
      .rename(options)

  private def printElement(element: ClassDiagramElement): String =
    element match {
      case UmlAbstractClass(name, _) =>
        s"abstract class ${quoteName(name)}"
      case UmlAnnotation(name, _, _) =>
        s"annotation ${quoteName(name)}"
      case UmlClass(name, _, _) =>
        s"class ${quoteName(name)}"
      case UmlEnum(name, _, _) =>
        s"enum ${quoteName(name)}"
      case UmlInterface(name, _) =>
        s"interface ${quoteName(name)}"
    }

  private def quoteName(name: String): String =
    if (SomewhatSensibleName.pattern.matcher(name).matches()) name
    else s""""$name""""
}
