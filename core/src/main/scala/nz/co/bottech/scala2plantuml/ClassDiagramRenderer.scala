package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml
import nz.co.bottech.scala2plantuml.ClassDiagramElement._
import nz.co.bottech.scala2plantuml.ClassDiagramRenderer.Options._

import java.io.{StringWriter, Writer}
import scala.annotation.tailrec

object ClassDiagramRenderer {

  private val SomewhatSensibleName = """[\p{Alnum}._-]+""".r

  final case class Options(
      companionObjects: CompanionObjectsOption,
      constructor: ConstructorOption,
      hide: HideOption,
      naming: NamingOption,
      sorting: SortingOption,
      syntheticMethods: SyntheticMethodsOption)

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  object Options {

    // Often used for constructors and other "constructors" like apply.
    val CreateStereotype: Option[String] = Some("Create")

    val JavaStdLibPattern: String  = scala2plantuml.JavaStdLibPattern
    val ScalaStdLibPattern: String = scala2plantuml.ScalaStdLibPattern

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

    def constructorTypeName(method: Method): String =
      scalaTypeName(symbolToScalaIdentifier(method.ownerSymbol))

    sealed trait CompanionObjectsOption
    case object CombineAsStatic extends CompanionObjectsOption
    case object SeparateClasses extends CompanionObjectsOption

    sealed trait ConstructorOption
    case object HideConstructors extends ConstructorOption

    final case class ShowConstructors(
        stereotype: Option[String] = None,
        name: Method => String = _.displayName)
        extends ConstructorOption

    sealed trait HideOption
    case object ShowAll extends HideOption
    // Pattern supports two wildcards:
    // - ** -> matches any character
    // - *  -> matches all characters except for '/'
    final case class HideMatching(patterns: Set[String] = Set(ScalaStdLibPattern, JavaStdLibPattern)) extends HideOption

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

  def renderString(elements: Seq[ClassDiagramElement], options: Options): String =
    writeAsString(render(elements, options, _))

  def render(elements: Seq[ClassDiagramElement], options: Options, writer: Writer): Unit = {
    writer.write("@startuml\n")
    renderSnippet(elements, options, writer)
    writer.write("@enduml\n")
  }

  def renderSnippetString(elements: Seq[ClassDiagramElement], options: Options): String =
    writeAsString(renderSnippet(elements, options, _))

  def renderSnippet(elements: Seq[ClassDiagramElement], options: Options, writer: Writer): Unit = {
    val elementsWithNames = DiagramModifications(elements, options)
    def owns(outer: Type, current: ClassDiagramElement): Boolean =
      current match {
        case member: Member => outer.owns(member)
        case _              => false
      }
    def nest(current: ClassDiagramElement, previous: Option[ClassDiagramElement], outer: Option[Type]): Boolean =
      outer match {
        case Some(out) => owns(out, current)
        case None =>
          previous match {
            case Some(prev: Type) => owns(prev, current)
            case _                => false
          }
      }

    @tailrec
    def loop(
        remaining: Seq[ClassDiagramElement],
        previous: Option[ClassDiagramElement],
        outer: Option[Type]
      ): Unit =
      remaining match {
        case head +: tail =>
          val nested = nest(head, previous, outer)
          if (outer.isEmpty) {
            if (nested) writer.write(" {")
          } else if (!nested) writer.write("\n}")
          if (previous.nonEmpty) writer.write('\n')
          if (nested) writer.write("  ")
          val nextOuter = if (nested) outer.orElse(previous).collect({ case typ: Type => typ }) else None
          writer.write(renderElementStart(head, nextOuter, elementsWithNames.names))
          loop(tail, Some(head), nextOuter)
        case Seq() =>
          if (outer.nonEmpty) writer.write("\n}")
          writer.write('\n')
      }

    loop(elementsWithNames.elements, None, None)
  }

  private def writeAsString(f: Writer => Unit): String = {
    val writer = new StringWriter()
    f(writer)
    writer.toString
  }

  private def renderElementStart(
      element: ClassDiagramElement,
      outer: Option[Type],
      names: Map[String, String]
    ): String =
    element match {
      case typ: Type                => renderType(typ, names)
      case member: Member           => renderMember(member, outer, names)
      case aggregation: Aggregation => renderAggregation(aggregation, names)
    }

  private def renderType(typ: Type, names: Map[String, String]): String = {
    val name           = renderTypeName(typ, names)
    val typeParameters = renderTypeParameters(typ.typeParameters, names)
    val extendsClause  = renderExtends(typ.parentSymbols, names, separator = ", ")
    val prefix = typ match {
      case _: Annotation => "annotation"
      case clazz: Class  => s"${renderAbstract(clazz)}class"
      case _: Enum       => "enum"
      case _: Interface  => "interface"
    }
    s"$prefix $name$typeParameters$extendsClause"
  }

  private def renderAbstract(clazz: Class): String =
    if (clazz.isAbstract) "abstract " else ""

  private def renderTypeParameters(
      parameters: Seq[ClassDiagramElement.TypeParameter],
      names: Map[String, String]
    ): String =
    if (parameters.nonEmpty) {
      val params = parameters.map { parameter =>
        val name          = renderSymbolName(parameter.symbol, names)
        val extendsClause = renderExtends(parameter.parentSymbols, names, separator = " & ")
        s"$name$extendsClause"
      }.mkString(", ")
      s"<$params>"
    } else ""

  private def renderExtends(symbols: Seq[String], names: Map[String, String], separator: String): String =
    if (symbols.nonEmpty) {
      val parents = symbols.map(renderSymbolName(_, names)).mkString(separator)
      s" extends $parents"
    } else ""

  private def quoteName(name: String): String =
    if (SomewhatSensibleName.pattern.matcher(name).matches()) name
    else s""""$name""""

  private def renderMember(member: Member, outer: Option[Type], names: Map[String, String]) = {
    val additionalModifier = renderAdditionalModifier(member, outer)
    val memberModifier     = renderMemberModifier(member)
    val visibility         = renderVisibility(member.visibility)
    val name               = renderMemberName(member, names)
    s"$visibility $additionalModifier{$memberModifier} $name"
  }

  private def renderMemberModifier(member: Member): String =
    member match {
      case _: Field  => "field"
      case _: Method => "method"
    }

  private def renderVisibility(visibility: Visibility): String =
    visibility match {
      case Visibility.Private        => "-"
      case Visibility.Protected      => "#"
      case Visibility.PackagePrivate => "~"
      case Visibility.Public         => "+"
    }

  private def renderAdditionalModifier(member: Member, outer: Option[Type]): String =
    if (outer.exists(_.isObject)) "{static} "
    else if (member.isAbstract) "{abstract} "
    else ""

  private def renderAggregation(aggregation: Aggregation, names: Map[String, String]): String = {
    val aggregatorName = renderSymbolName(aggregation.aggregator, names)
    val aggregatedName = renderSymbolName(aggregation.aggregated, names)
    s"$aggregatorName o-- $aggregatedName"
  }

  private def renderTypeName(typ: Type, names: Map[String, String]): String =
    quoteName(names.getOrElse(typ.symbol, typ.displayName))

  private def renderMemberName(definition: Member, names: Map[String, String]): String =
    names.getOrElse(definition.symbol, definition.displayName)

  private def renderSymbolName(symbol: String, names: Map[String, String]): String =
    quoteName(names.getOrElse(symbol, symbolToScalaIdentifier(symbol)))
}
