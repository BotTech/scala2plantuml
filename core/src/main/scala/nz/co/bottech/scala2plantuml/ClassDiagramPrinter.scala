package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramElement._
import nz.co.bottech.scala2plantuml.ClassDiagramPrinter.Options._

import scala.annotation.tailrec

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
      scalaTypeName(symbolToScalaIdentifier(element.ownerSymbol))

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
    val elementsWithNames = DiagramModifications(elements, options)
    val builder           = new StringBuilder
    def nest(outer: ClassDiagramElement, current: ClassDiagramElement): Boolean =
      current.isMember && outer.owns(current)
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
          val nextOuter = if (nested) outer.orElse(previous) else None
          builder.append(printElementStart(head, nextOuter, elementsWithNames.names))
          loop(tail, Some(head), nextOuter)
        case Seq() =>
          if (outer.nonEmpty) builder.append("\n}")
          builder.append('\n')
          builder.toString
      }

    loop(elementsWithNames.elements, None, None)
  }

  private def printElementStart(
      element: ClassDiagramElement,
      outer: Option[ClassDiagramElement],
      names: Map[String, String]
    ): String =
    element match {
      case typ: Type      => printType(typ, names)
      case member: Member => printMember(member, outer, names)
    }

  private def printType(typ: Type, names: Map[String, String]): String = {
    val name           = quoteName(printName(typ, names))
    val typeParameters = printTypeParameters(typ.typeParameters, names)
    val extendsClause  = printExtends(typ.parentSymbols, names, separator = ", ")
    val prefix = typ match {
      case _: Annotation => "annotation"
      case clazz: Class  => s"${printAbstract(clazz)}class"
      case _: Enum       => "enum"
      case _: Interface  => "interface"
    }
    s"$prefix $name$typeParameters$extendsClause"
  }

  private def printAbstract(clazz: Class): String =
    if (clazz.isAbstract) "abstract " else ""

  private def printTypeParameters(
      parameters: Seq[ClassDiagramElement.TypeParameter],
      names: Map[String, String]
    ): String =
    if (parameters.nonEmpty) {
      val params = parameters.map { parameter =>
        val name          = quoteName(names.getOrElse(parameter.symbol, parameter.symbol))
        val extendsClause = printExtends(parameter.parentSymbols, names, separator = " & ")
        s"$name$extendsClause"
      }.mkString(", ")
      s"<$params>"
    } else ""

  private def printExtends(symbols: Seq[String], names: Map[String, String], separator: String): String =
    if (symbols.nonEmpty) {
      val parents = symbols.map { symbol =>
        quoteName(names.getOrElse(symbol, symbolToScalaIdentifier(symbol)))
      }.mkString(separator)
      s" extends $parents"
    } else ""

  private def quoteName(name: String): String =
    if (SomewhatSensibleName.pattern.matcher(name).matches()) name
    else s""""$name""""

  private def printMember(member: Member, outer: Option[ClassDiagramElement], names: Map[String, String]) = {
    val additionalModifier = printAdditionalModifier(member, outer)
    val memberModifier     = printMemberModifier(member)
    val visibility         = printVisibility(member.visibility)
    val name               = printName(member, names)
    s"$visibility $additionalModifier{$memberModifier} $name"
  }

  private def printName(element: ClassDiagramElement, names: Map[String, String]) =
    names.getOrElse(element.symbol, element.displayName)

  private def printMemberModifier(member: Member): String =
    member match {
      case _: Field  => "field"
      case _: Method => "method"
    }

  private def printVisibility(visibility: Visibility): String =
    visibility match {
      case Visibility.Private        => "-"
      case Visibility.Protected      => "#"
      case Visibility.PackagePrivate => "~"
      case Visibility.Public         => "+"
    }

  private def printAdditionalModifier(member: Member, outer: Option[ClassDiagramElement]): String =
    if (outer.exists(_.isObject)) "{static} "
    else if (member.isAbstract) "{abstract} "
    else ""
}
