package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramElement._
import nz.co.bottech.scala2plantuml.ClassDiagramRenderer.Options.Unsorted
import nz.co.bottech.scala2plantuml.ClassDiagramRenderer._

import scala.annotation.tailrec

private[scala2plantuml] object DiagramModifications {

  final case class ElementsWithNames(elements: Seq[ClassDiagramElement], names: Map[String, String])

  //noinspection MutatorLikeMethodIsParameterless
  implicit class FluidOptions(val elementsWithNames: ElementsWithNames) extends AnyVal {

    import elementsWithNames._

    def removeHidden(options: Options): ElementsWithNames =
      options.hide match {
        case Options.HideMatching(patterns) =>
          val tests                               = patterns.map(patternToRegex(_).asMatchPredicate)
          def hideSymbol(symbol: String): Boolean = tests.exists(_.test(symbol))
          def hideElement(element: ClassDiagramElement): Boolean =
            element match {
              case definition: Definition   => hideSymbol(definition.symbol)
              case aggregation: Aggregation => hideSymbol(aggregation.aggregator) || hideSymbol(aggregation.aggregated)
              case _                        => false
            }
          def hideTypeParameters(typeParameters: Seq[TypeParameter]) =
            typeParameters
              .filterNot(parameter => hideSymbol(parameter.symbol))
              .map(parameter => parameter.copy(parentSymbols = parameter.parentSymbols.filterNot(hideSymbol)))
          val newElements = elements.filterNot(element => hideElement(element)).map {
            case typ: Type =>
              val newParents        = typ.parentSymbols.filterNot(hideSymbol)
              val newTypeParameters = hideTypeParameters(typ.typeParameters)
              typ match {
                case annotation: Annotation =>
                  annotation.copy(parentSymbols = newParents, typeParameters = newTypeParameters)
                case clazz: Class =>
                  clazz.copy(parentSymbols = newParents, typeParameters = newTypeParameters)
                case enum: Enum =>
                  enum.copy(parentSymbols = newParents, typeParameters = newTypeParameters)
                case interface: Interface =>
                  interface.copy(parentSymbols = newParents, typeParameters = newTypeParameters)
              }
            case method: Method =>
              val newTypeParameters = hideTypeParameters(method.typeParameters)
              method.copy(typeParameters = newTypeParameters)
            case element => element
          }
          elementsWithNames.copy(elements = newElements)
        case Options.ShowAll => elementsWithNames
      }

    def removeSynthetics(options: Options): ElementsWithNames =
      options.syntheticMethods match {
        case Options.HideSyntheticMethods =>
          val newElements = elements.filterNot {
            case method: Method => !method.constructor && method.synthetic
            case _              => false
          }
          elementsWithNames.copy(elements = newElements)
        case Options.ShowSyntheticMethods => elementsWithNames
      }

    def removeDuplicateAggregations: ElementsWithNames = {
      @tailrec
      def loop(
          remaining: Seq[ClassDiagramElement],
          aggregations: Set[Aggregation],
          acc: Vector[ClassDiagramElement]
        ): Vector[ClassDiagramElement] =
        remaining match {
          case (aggregation: Aggregation) +: tail if aggregations.contains(aggregation) =>
            loop(tail, aggregations, acc)
          case (aggregation: Aggregation) +: tail =>
            loop(tail, aggregations + aggregation, acc :+ aggregation)
          case head +: tail =>
            loop(tail, aggregations, acc :+ head)
          case Seq() => acc
        }
      val newElements = loop(elements, Set.empty, Vector.empty)
      elementsWithNames.copy(elements = newElements)
    }

    def addMissingElements(options: Options): ElementsWithNames =
      options.sorting match {
        case Unsorted =>
          // Fields may appear before their parents so we may need to insert duplicate parents and we have to do that
          // before other operations like renaming.
          val types = elements.collect({ case typ: Type => typ }).map(element => element.symbol -> element).toMap
          @tailrec
          def loop(
              remaining: Seq[ClassDiagramElement],
              previousType: Option[Type],
              acc: Vector[ClassDiagramElement]
            ): Vector[ClassDiagramElement] =
            remaining match {
              case (typ: Type) +: tail =>
                val nextAcc = if (previousType.exists(_.symbol == typ.symbol)) acc else acc :+ typ
                loop(tail, Some(typ), nextAcc)
              case (member: Member) +: tail =>
                val owner = member.ownerSymbol
                if (previousType.exists(_.symbol == owner)) loop(tail, previousType, acc :+ member)
                else {
                  def createClass =
                    Class(
                      scalaTypeName(symbolToScalaIdentifier(owner)),
                      owner,
                      isObject = false,
                      isAbstract = false,
                      Seq.empty,
                      Seq.empty
                    )
                  val ownerElement = types.getOrElse(owner, createClass)
                  loop(tail, Some(ownerElement), acc :+ ownerElement :+ member)
                }
              case head +: tail =>
                loop(tail, previousType, acc :+ head)
              case _ => acc
            }
          val newElements = loop(elements, None, Vector.empty)
          elementsWithNames.copy(elements = newElements)
        case _ => elementsWithNames
      }

    def calculateNames(options: Options): ElementsWithNames = {
      assert(names.isEmpty)
      def typeParameterSymbols(parameters: Seq[TypeParameter]): Seq[String] =
        parameters.flatMap(parameter => parameter.symbol +: parameter.parentSymbols)
      // TODO: This might be overkill. All the symbols we care about should be types.
      def elementSymbols(element: ClassDiagramElement): Seq[String] =
        element match {
          case typ: Type                => typ.symbol +: typ.parentSymbols ++: typeParameterSymbols(typ.typeParameters)
          case field: Field             => Seq(field.symbol)
          case method: Method           => method.symbol +: typeParameterSymbols(method.typeParameters)
          case aggregation: Aggregation => Seq(aggregation.aggregator, aggregation.aggregated)
        }
      def elementNames(element: ClassDiagramElement, f: String => String): Seq[(String, String)] = {
        def symbolName(symbol: String) = symbol -> symbolToScalaIdentifier(f(symbol)).replace("`", "")
        element match {
          case typ: Type =>
            val elementName = symbolName(typ.symbol)
            val typeParameterNames = typ.typeParameters.flatMap { parameter =>
              symbolName(parameter.symbol) +: parameter.parentSymbols.map(symbolName)
            }
            elementName +: typeParameterNames
          case member: Member => Seq(member.symbol -> member.displayName)
          case _: Aggregation => Seq.empty
        }
      }
      val newNames = options.naming match {
        case Options.FullyQualified =>
          elements.flatMap(element => elementNames(element, identity)).toMap
        case Options.RemoveCommonPrefix =>
          val symbols = elements.flatMap(elementSymbols).toSet
          val firstPrefix = symbols.headOption.map { symbol =>
            val i = symbol.lastIndexOf('/')
            symbol.take(i + 1)
          }.getOrElse("")
          val commonPrefix = symbols.foldLeft(firstPrefix) {
            case (prefix, symbol) => longestPrefix(prefix, symbol)
          }
          elements.flatMap(element => elementNames(element, _.drop(commonPrefix.length))).toMap
      }
      elementsWithNames.copy(names = newNames)
    }

    def combineCompanionObjects(options: Options): ElementsWithNames =
      options.companionObjects match {
        case Options.CombineAsStatic =>
          val nonObjectTypeNames = elements.collect {
            case typ: Type if !typ.isObject => symbolToScalaIdentifier(typ.symbol)
          }.toSet
          val newElements = elements.collect {
            case typ: Type if !typ.isObject                                                     => typ
            case typ: Type if !nonObjectTypeNames.contains(symbolToScalaIdentifier(typ.symbol)) => typ
            case member: Member                                                                 => member
            case aggregation: Aggregation                                                       => aggregation
          }
          elementsWithNames.copy(elements = newElements)
        case Options.SeparateClasses =>
          val newNames = elements.foldLeft(names) {
            case (names, typ: Type) if typ.isObject =>
              val name = names.getOrElse(typ.symbol, typ.displayName)
              names.updated(typ.symbol, s"$name$$")
            case (names, _) => names
          }
          elementsWithNames.copy(names = newNames)
      }

    def updateConstructors(options: Options): ElementsWithNames =
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

    def sort(options: Options): ElementsWithNames = {
      val newElements = options.sorting match {
        case Options.NaturalSortOrder => elements.sorted(new ClassDiagramElementOrdering(NaturalTypeOrdering))
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

  def apply(elements: Seq[ClassDiagramElement], options: Options): ElementsWithNames =
    ElementsWithNames(
      elements,
      Map.empty
    ).removeHidden(options)
      .removeSynthetics(options)
      .removeDuplicateAggregations
      .addMissingElements(options)
      .calculateNames(options)
      .combineCompanionObjects(options)
      .updateConstructors(options)
      .sort(options)
}
