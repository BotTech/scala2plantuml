package nz.co.bottech.scala2plantuml

import scala.annotation.tailrec

object ClassDiagramPrinter {

  private val SomewhatSensibleName = """[\p{Alnum}_-]+""".r

  final case class Options(
      namingStrategy: NamingStrategy,
      combineCompanionObject: Boolean)

  object Options {

    val default: Options = Options(
      namingStrategy = RemoveCommonPrefix,
      combineCompanionObject = true
    )
  }

  sealed trait NamingStrategy
  case object FullQualified      extends NamingStrategy
  case object RemoveCommonPrefix extends NamingStrategy

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

  private def applyOptions(elements: Seq[ClassDiagramElement], options: Options): Seq[ClassDiagramElement] =
    if (options.combineCompanionObject) {
      val nonObjectNames = elements.filterNot(_.isObject).map(_.fullName).toSet
      elements.filterNot(element => element.isObject && nonObjectNames.contains(element.fullName))
    } else
      elements

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
