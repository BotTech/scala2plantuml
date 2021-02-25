package nz.co.bottech.scala2plantuml

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
      scalaTypeName(symbolToScalaIdentifier(methodParent(element.symbol)))

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
    def endPrevious(previous: String, next: Option[String], outer: List[String]): List[String] =
      if (next.exists(_.startsWith(previous))) {
        // This is nested under the previous so create an outer scope.
        builder.append(" {\n")
        previous +: outer
      } else {
        builder.append("\n")
        outer
      }
    @tailrec
    def closeOuter(next: Option[String], outer: List[String]): List[String] =
      outer match {
        case head :: tail if !next.exists(_.startsWith(head)) =>
          // No next or it is not nested under the outer element so close the outer one.
          builder.append("  " * tail.size)
          builder.append("}\n")
          closeOuter(next, tail)
        case _ => outer
      }
    @tailrec
    def loop(remaining: Seq[ClassDiagramElement], previous: Option[String], outer: List[String]): String = {
      val maybeCurrent = remaining.headOption.map(_.symbol)
      val nextOuter = closeOuter(
        maybeCurrent,
        previous
          .map(endPrevious(_, maybeCurrent, outer))
          .getOrElse(outer)
      )
      remaining match {
        case head +: tail =>
          builder.append("  " * nextOuter.size)
          builder.append(printElementStart(head))
          loop(tail, Some(head.symbol), nextOuter)
        case Seq() =>
          builder.toString
      }
    }

    val updatedElements = DiagramModifications(elements, options)
    loop(updatedElements, None, Nil)
  }

  def methodParent(symbol: String): String =
    symbol.desc match {
      case _: Descriptor.Method => symbol.ownerChain.takeRight(2).headOption.getOrElse(symbol)
      case _                    => symbol
    }

  def symbolToScalaIdentifier(symbol: String): String =
    if (symbol.isGlobal) symbol.replace('/', '.').dropRight(1)
    else symbol.replace('/', '.')

  def scalaTypeName(identifier: String): String =
    identifier.split('.').last.split('#').head

  private def printElementStart(element: ClassDiagramElement): String =
    element match {
      case uml: UmlAbstractClass =>
        s"abstract class ${quoteName(uml.displayName)}"
      case uml: UmlAnnotation =>
        s"annotation ${quoteName(uml.displayName)}"
      case uml: UmlClass =>
        s"class ${quoteName(uml.displayName)}"
      case uml: UmlEnum =>
        s"enum ${quoteName(uml.displayName)}"
      case uml: UmlField =>
        s"${printVisibility(uml.visibility)} {field} ${uml.displayName}"
      case uml: UmlInterface =>
        s"interface ${quoteName(uml.displayName)}"
      case uml: UmlMethod =>
        s"${printVisibility(uml.visibility)} {method} ${uml.displayName}"
    }

  private def printVisibility(visibility: UmlVisibility): String =
    visibility match {
      case UmlVisibility.Private        => "-"
      case UmlVisibility.Protected      => "#"
      case UmlVisibility.PackagePrivate => "~"
      case UmlVisibility.Public         => "+"
    }

  private def quoteName(name: String): String =
    if (SomewhatSensibleName.pattern.matcher(name).matches()) name
    else s""""$name""""
}
