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
      naming: NamingOption,
      sorting: SortingOption,
      syntheticMethods: SyntheticMethodsOption)

  object Options {

    // Often used for constructors and other "constructors" like apply.
    val CreateStereotype: Option[String] = Some("Create")

    val Default: Options = Options(
      companionObjects = CombineAsStatic,
      constructor = ShowConstructors(),
      naming = RemoveCommonPrefix,
      sorting = NaturalSortOrder,
      syntheticMethods = HideSyntheticMethods
    )

    val Minimal: Options = Options(
      companionObjects = CombineAsStatic,
      constructor = HideConstructors,
      naming = RemoveCommonPrefix,
      sorting = NaturalSortOrder,
      syntheticMethods = HideSyntheticMethods
    )

    val Verbose: Options = Options(
      companionObjects = SeparateClasses,
      constructor = ShowConstructors(CreateStereotype, constructorTypeName),
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

    case class ShowConstructors(stereotype: Option[String] = None, name: ClassDiagramElement => String = _.displayName)
        extends ConstructorOption

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
      val maybeNext = remaining.headOption.map(_.symbol)
      val nextOuter = closeOuter(
        maybeNext,
        previous
          .map(endPrevious(_, maybeNext, outer))
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

    val updatedElements = applyOptions(elements, options)
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

  implicit private class FluidOptions(val elements: Seq[ClassDiagramElement]) extends AnyVal {

    def removeSynthetics(options: Options): Seq[ClassDiagramElement] =
      options.syntheticMethods match {
        case Options.HideSyntheticMethods =>
          elements.filterNot {
            case method: UmlMethod => !method.constructor && method.synthetic
            case _                 => false
          }
        case Options.ShowSyntheticMethods =>
          elements
      }

    def combineCompanionObjects(options: Options): Seq[ClassDiagramElement] =
      options.companionObjects match {
        case Options.CombineAsStatic =>
          val nonObjectNames =
            elements.filterNot(_.isObject).map(element => symbolToScalaIdentifier(element.symbol)).toSet
          elements.filterNot(element =>
            element.isObject && nonObjectNames.contains(symbolToScalaIdentifier(element.symbol))
          )
        case Options.SeparateClasses =>
          elements.map { element =>
            if (element.isObject) renameElement(element, _.displayName + "$")
            else element
          }
      }

    def rename(options: Options): Seq[ClassDiagramElement] =
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

    def updateConstructors(options: Options): Seq[ClassDiagramElement] =
      options.constructor match {
        case Options.HideConstructors =>
          elements.filterNot {
            case method: UmlMethod => method.constructor
            case _                 => false
          }
        case Options.ShowConstructors(stereotype, name) =>
          elements.map {
            case method: UmlMethod if method.constructor =>
              val newName            = name(method)
              val nameWithStereotype = stereotype.map(s => s"<<$s>> $newName").getOrElse(newName)
              method.copy(displayName = nameWithStereotype)
            case element => element
          }
      }

    def addMissingElements(options: Options): Seq[ClassDiagramElement] =
      // Fields may appear before their parents so we may need to insert duplicate parents and we have to do that
      // before other operations like renaming.
      options.sorting match {
        case Unsorted =>
          val types = elements.filter(_.symbol.isType).map(element => element.symbol -> element).toMap
          val (_, withMissing) = elements.foldLeft((List.empty[String], Vector.empty[ClassDiagramElement])) {
            case ((outer, acc), element) =>
              if (isMember(element)) {
                val parent       = methodParent(element.symbol)
                def createParent = UmlClass(scalaTypeName(symbolToScalaIdentifier(parent)), parent, isObject = false)
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

    def sort(options: Options): Seq[ClassDiagramElement] =
      options.sorting match {
        case Options.NaturalSortOrder => elements.sortBy(_.symbol)(new NaturalOrdering)
        case Options.Unsorted         => elements
      }

    private def longestPrefix(a: String, b: String): String = {
      val i = (0 until math.min(a.length, b.length)).findLast { i =>
        a(i) == b(i)
      }.getOrElse(-1)
      a.take(i + 1)
    }

    private def renameElement(element: ClassDiagramElement, f: ClassDiagramElement => String): ClassDiagramElement =
      element match {
        case element: UmlAbstractClass => element.copy(displayName = f(element))
        case element: UmlAnnotation    => element.copy(displayName = f(element))
        case element: UmlEnum          => element.copy(displayName = f(element))
        case element: UmlField         => element
        case element: UmlClass         => element.copy(displayName = f(element))
        case element: UmlInterface     => element.copy(displayName = f(element))
        case element: UmlMethod        => element
      }
  }

  private def applyOptions(elements: Seq[ClassDiagramElement], options: Options): Seq[ClassDiagramElement] =
    elements
      .removeSynthetics(options)
      .addMissingElements(options)
      .combineCompanionObjects(options)
      .rename(options)
      .updateConstructors(options)
      .sort(options)

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

  private def isMember(element: ClassDiagramElement): Boolean =
    element match {
      case _: UmlField  => true
      case _: UmlMethod => true
      case _            => false
    }
}
