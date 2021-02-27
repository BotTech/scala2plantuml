package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramElement._
import nz.co.bottech.scala2plantuml.ClassDiagramPrinter.Options._

import scala.annotation.tailrec
import scala.meta.internal.semanticdb.Scala._

object ClassDiagramPrinter {

  // TODO: Use java.lang.Character.isJavaIdentifierPart(char)
  //  See scala.meta.internal.semanticdb.Scala.Names.encode
  private val SomewhatSensibleName = """[\p{Alnum}._-]+""".r

  final case class Options(
      companionObjects: CompanionObjectsOption,
      constructor: ConstructorOption,
      hide: HideOption,
      naming: NamingOption,
      sorting: SortingOption,
      syntheticMethods: SyntheticMethodsOption)

  object Options {

    // Often used for constructors and other "constructors" like apply.
    val CreateStereotype: Option[String] = Some("Create")

    val ScalaStdLibPattern: String = "scala/**"

    val Default: Options = Options(
      companionObjects = CombineAsStatic,
      constructor = ShowConstructors(),
      hide = HideMatching(),
      naming = RemoveCommonPrefix,
      sorting = NaturalSortOrder,
      syntheticMethods = HideSyntheticMethods
    )

    val Minimal: Options = Options(
      companionObjects = CombineAsStatic,
      constructor = HideConstructors,
      hide = HideMatching(),
      naming = RemoveCommonPrefix,
      sorting = NaturalSortOrder,
      syntheticMethods = HideSyntheticMethods
    )

    val Verbose: Options = Options(
      companionObjects = SeparateClasses,
      constructor = ShowConstructors(CreateStereotype, constructorTypeName),
      hide = ShowAll,
      naming = FullyQualified,
      sorting = Unsorted,
      syntheticMethods = ShowSyntheticMethods
    )

    def constructorTypeName(element: ClassDiagramElement): String =
      scalaTypeName(symbolToScalaIdentifier(element.parentSymbol))

    sealed trait CompanionObjectsOption
    case object CombineAsStatic extends CompanionObjectsOption
    case object SeparateClasses extends CompanionObjectsOption

    sealed trait ConstructorOption
    case object HideConstructors extends ConstructorOption

    final case class ShowConstructors(
        stereotype: Option[String] = None,
        name: ClassDiagramElement => String = _.displayName)
        extends ConstructorOption

    sealed trait HideOption
    case object ShowAll extends HideOption
    // Pattern supports two wildcards:
    // - ** -> matches any character
    // - *  -> matches all characters except for '/'
    final case class HideMatching(patterns: Set[String] = Set(ScalaStdLibPattern)) extends HideOption

    sealed trait NamingOption
    case object FullyQualified     extends NamingOption
    case object RemoveCommonPrefix extends NamingOption

    sealed trait SortingOption
    case object NaturalSortOrder extends SortingOption
    case object Unsorted         extends SortingOption

    sealed trait SyntheticMethodsOption
    case object HideSyntheticMethods extends SyntheticMethodsOption
    case object ShowSyntheticMethods extends SyntheticMethodsOption
  }

  def print(elements: Seq[ClassDiagramElement], options: Options): String =
    s"""@startuml
       |${printSnippet(elements, options)}
       |@enduml""".stripMargin

  def printSnippet(elements: Seq[ClassDiagramElement], options: Options): String = {
    val builder = new StringBuilder
    def nest(outer: ClassDiagramElement, current: ClassDiagramElement): Boolean =
      current.isMember && outer.isParentOf(current)
    @tailrec
    def loop(
        remaining: Seq[ClassDiagramElement],
        previous: Option[ClassDiagramElement],
        outer: Option[ClassDiagramElement]
      ): String =
      remaining match {
        case head +: tail =>
          val nested = outer match {
            case Some(out) => nest(out, head)
            case None =>
              previous match {
                case Some(prev) => nest(prev, head)
                case None       => false
              }
          }
          if (outer.isEmpty) {
            if (nested) builder.append(" {")
          } else if (!nested) builder.append("\n}")
          builder.append('\n')
          if (nested) builder.append("  ")
          builder.append(printElementStart(head))
          val nextOuter = if (nested) outer.orElse(previous) else None
          loop(tail, Some(head), nextOuter)
        case Seq() =>
          if (outer.nonEmpty) builder.append("\n}")
          builder.append('\n')
          builder.toString
      }

    val updatedElements = DiagramModifications(elements, options)
    loop(updatedElements, None, None)
  }

  def symbolToScalaIdentifier(symbol: String): String =
    if (symbol.isGlobal) symbol.replace('/', '.').dropRight(1)
    else symbol.replace('/', '.')

  def scalaTypeName(identifier: String): String =
    identifier.split('.').last.split('#').head

  private def printElementStart(element: ClassDiagramElement): String =
    element match {
      case uml: AbstractClass =>
        s"abstract class ${quoteName(uml.displayName)}"
      case uml: Annotation =>
        s"annotation ${quoteName(uml.displayName)}"
      case uml: Class =>
        s"class ${quoteName(uml.displayName)}"
      case uml: Enum =>
        s"enum ${quoteName(uml.displayName)}"
      case uml: Field =>
        s"${printVisibility(uml.visibility)} {field} ${uml.displayName}"
      case uml: Interface =>
        s"interface ${quoteName(uml.displayName)}"
      case uml: Method =>
        s"${printVisibility(uml.visibility)} {method} ${uml.displayName}"
    }

  private def printVisibility(visibility: Visibility): String =
    visibility match {
      case Visibility.Private        => "-"
      case Visibility.Protected      => "#"
      case Visibility.PackagePrivate => "~"
      case Visibility.Public         => "+"
    }

  private def quoteName(name: String): String =
    if (SomewhatSensibleName.pattern.matcher(name).matches()) name
    else s""""$name""""
}
