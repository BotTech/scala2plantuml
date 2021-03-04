package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.AdditionalSymbolInformation._
import nz.co.bottech.scala2plantuml.ClassDiagramElement.{Annotation => UMLAnnotation, Type => _, _}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.collection.immutable.HashSet
import scala.meta.internal.semanticdb.Scala._
import scala.meta.internal.semanticdb._

private[scala2plantuml] object SymbolProcessor {

  private val TerminalTypes = Set("scala/Any#", "scala/Nothing#")

  private val Logger = LoggerFactory.getLogger(getClass.getName.dropRight(1))

  // TODO: Perhaps we should return a different type than the types used for rendering.
  def processSymbol(
      symbol: String,
      symbolIndex: SymbolIndex,
      typeIndex: TypeIndex,
      definitionIndex: DefinitionIndex
    ): Seq[ClassDiagramElement] = {
    def skip(symbol: String, seen: HashSet[String]): Boolean =
      seen.contains(symbol) || !symbolIndex.indexOf(symbol)
    @tailrec
    def loop(
        remaining: Vector[String],
        seen: HashSet[String],
        acc: Vector[ClassDiagramElement]
      ): Vector[ClassDiagramElement] = {
      checkIfInterrupted()
      remaining match {
        case current +: tail if skip(current, seen) =>
          loop(tail, seen, acc)
        case current +: tail =>
          symbolIndex.lookup(current) match {
            case Some(symbolInformation: SymbolInformation) =>
              Logger.trace(symbolInformationString(symbolInformation))
              val elements = symbolElements(symbolInformation, symbolIndex, typeIndex, definitionIndex)
              // Traverse breadth-first so that we process a full symbol before moving onto the next.
              val nextSymbols = tail ++ symbolReferences(symbolInformation)
              val nextSeen    = seen + current
              loop(nextSymbols, nextSeen, elements ++: acc)
            case None =>
              Logger.warn(s"Missing symbol: $current")
              loop(tail, seen, acc)
          }
        case _ => acc
      }
    }

    loop(Vector(symbol), HashSet.empty, Vector.empty)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  private def checkIfInterrupted(): Unit =
    if (Thread.currentThread().isInterrupted)
      throw new InterruptedException("Interrupted while processing symbol.")

  private def symbolInformationString(symbol: SymbolInformation): String =
    s"""SymbolInformation(
       |  language: ${symbol.language}
       |  symbol: ${symbol.symbol}
       |  kind: ${symbol.kind}
       |  display_name: ${symbol.displayName}
       |  signature: ${symbol.signature}
       |)""".stripMargin

  private def symbolElements(
      symbolInformation: SymbolInformation,
      symbolIndex: SymbolIndex,
      typeIndex: TypeIndex,
      definitionIndex: DefinitionIndex
    ): Seq[ClassDiagramElement] =
    symbolInformation.signature match {
      case Signature.Empty         => Seq.empty
      case clazz: ClassSignature   => classElement(symbolInformation, clazz, symbolIndex, typeIndex)
      case method: MethodSignature => methodElements(symbolInformation, method, symbolIndex, definitionIndex)
      case _: TypeSignature        => Seq.empty // We can't do much with this because we don't know the aggregator.
      case _: ValueSignature       => Seq.empty // Same here.
    }

  private def classElement(
      symbolInformation: SymbolInformation,
      clazz: ClassSignature,
      symbolIndex: SymbolIndex,
      typeIndex: TypeIndex
    ): Seq[ClassDiagramElement] = {
    import symbolInformation.{displayName, symbol}
    val parentSymbols  = clazz.parents.flatMap(typeSymbols(_, includeArguments = false))
    val typeParameters = optionalScopeTypeParameters(clazz.typeParameters, symbolIndex)
    val element: ClassDiagramElement.Type =
      if (isTrait(symbolInformation))
        Interface(displayName, symbol, parentSymbols, typeParameters)
      else if (isAnnotation(symbolInformation, typeIndex))
        UMLAnnotation(displayName, symbol, isObject(symbolInformation), parentSymbols, typeParameters)
      else if (isEnum(symbolInformation, typeIndex))
        Enum(displayName, symbol, isObject(symbolInformation), parentSymbols, typeParameters)
      else
        Class(
          displayName,
          symbol,
          isObject(symbolInformation),
          isAbstract(symbolInformation),
          parentSymbols,
          typeParameters
        )
    val aggregator                = aggregatorSymbol(symbolInformation)
    val typeParameterAggregations = optionalScopeAggregations(aggregator, clazz.typeParameters, symbolIndex)
    val parentAggregations        = clazz.parents.flatMap(typeArgumentAggregations(aggregator, _, symbolIndex))
    element +: typeParameterAggregations ++: parentAggregations
  }

  private def methodElements(
      symbolInformation: SymbolInformation,
      method: MethodSignature,
      symbolIndex: SymbolIndex,
      definitionIndex: DefinitionIndex
    ): Seq[ClassDiagramElement] = {
    import symbolInformation.{displayName, symbol}
    val visibility = symbolVisibility(symbolInformation)
    val aggregator = aggregatorSymbol(symbolInformation)
    if (isField(symbolInformation)) {
      val element = Field(displayName, symbol, visibility, isAbstract(symbolInformation))
      val returnTypeAggregations: Seq[ClassDiagramElement] =
        typeAggregations(aggregator, method.returnType, symbolIndex)
      element +: returnTypeAggregations
    } else {
      val typeParameters = optionalScopeTypeParameters(method.typeParameters, symbolIndex)
      val element = Method(
        displayName,
        symbol,
        visibility,
        isConstructor(symbolInformation),
        isSynthetic(symbolInformation.symbol, definitionIndex),
        isAbstract(symbolInformation),
        typeParameters
      )
      val typeParameterAggregations = optionalScopeAggregations(aggregator, method.typeParameters, symbolIndex)
      val parameterAggregations     = method.parameterLists.flatMap(scopeAggregations(aggregator, _, symbolIndex))
      val returnTypeAggregations: Seq[ClassDiagramElement] =
        typeAggregations(aggregator, method.returnType, symbolIndex)
      element +: typeParameterAggregations ++: parameterAggregations ++: returnTypeAggregations
    }
  }

  private def symbolReferences(symbolInformation: SymbolInformation): Seq[String] =
    symbolInformation.signature match {
      case Signature.Empty         => Seq.empty
      case value: ValueSignature   => valueReferences(value)
      case clazz: ClassSignature   => classReferences(clazz)
      case method: MethodSignature => methodReferences(method)
      case typ: TypeSignature      => typeSignatureReferences(typ)
    }

  private def valueReferences(value: ValueSignature): Seq[String] =
    typeReferences(value.tpe)

  private def classReferences(clazz: ClassSignature): Seq[String] =
    typeReferences(clazz.self) ++
      clazz.parents.flatMap(typeReferences) ++
      optionalScopeReferences(clazz.typeParameters) ++
      optionalScopeReferences(clazz.declarations)

  private def methodReferences(method: MethodSignature): Seq[String] =
    optionalScopeReferences(method.typeParameters) ++
      method.parameterLists.flatMap(scopeReferences) ++
      typeReferences(method.returnType)

  private def typeSignatureReferences(typ: TypeSignature): Seq[String] =
    optionalScopeReferences(typ.typeParameters) ++
      typeReferences(typ.lowerBound) ++
      typeReferences(typ.upperBound)

  private def optionalScopeReferences(maybeScope: Option[Scope]): Seq[String] =
    maybeScope.map(scopeReferences).getOrElse(Seq.empty)

  private def scopeReferences(scope: Scope): Seq[String] =
    scope.symlinks ++ scope.hardlinks.flatMap(symbolReferences)

  // scalastyle:off cyclomatic.complexity
  private def typeReferences(typ: Type): Seq[String] =
    typ match {
      case Type.Empty       => Seq.empty
      case WithType(types)  => types.flatMap(typeReferences)
      case UnionType(types) => types.flatMap(typeReferences)
      case _: ConstantType  =>
        // TODO: Do something better with constant types.
        Seq.empty
      case RepeatedType(tpe)                  => typeReferences(tpe)
      case ExistentialType(tpe, declarations) =>
        // TODO: Show existential types as their own type.
        typeReferences(tpe) ++ optionalScopeReferences(declarations)
      case TypeRef(_, symbol, typeArguments) =>
        symbol +: typeArguments.flatMap(typeReferences)
      case SingleType(_, symbol)              => Seq(symbol)
      case UniversalType(typeParameters, tpe) => typeReferences(tpe) ++ optionalScopeReferences(typeParameters)
      case IntersectionType(types)            => types.flatMap(typeReferences)
      case ByNameType(tpe)                    => typeReferences(tpe)
      case ThisType(symbol)                   => Seq(symbol)
      case AnnotatedType(annotations, tpe) =>
        typeReferences(tpe) ++ annotations.flatMap(annotation => typeReferences(annotation.tpe))
      case SuperType(_, symbol)              => Seq(symbol)
      case StructuralType(tpe, declarations) => typeReferences(tpe) ++ optionalScopeReferences(declarations)
    }
  // scalastyle:on cyclomatic.complexity

  // scalastyle:off cyclomatic.complexity
  private def typeSymbols(typ: Type, includeArguments: Boolean): Seq[String] =
    typ match {
      case Type.Empty       => Seq.empty
      case WithType(types)  => types.flatMap(typeSymbols(_, includeArguments))
      case UnionType(types) => types.flatMap(typeSymbols(_, includeArguments))
      case _: ConstantType  =>
        // TODO: Do something better with constant types.
        Seq.empty
      case RepeatedType(tpe)       => typeSymbols(tpe, includeArguments)
      case ExistentialType(tpe, _) => typeSymbols(tpe, includeArguments)
      case TypeRef(_, symbol, typeArguments) if includeArguments =>
        symbol +: typeArguments.flatMap(typeSymbols(_, includeArguments))
      case TypeRef(_, symbol, _)   => Seq(symbol)
      case SingleType(_, symbol)   => Seq(symbol)
      case UniversalType(_, tpe)   => typeSymbols(tpe, includeArguments)
      case IntersectionType(types) => types.flatMap(typeSymbols(_, includeArguments))
      case ByNameType(tpe)         => typeSymbols(tpe, includeArguments)
      case ThisType(symbol)        => Seq(symbol)
      case AnnotatedType(_, tpe)   => typeSymbols(tpe, includeArguments)
      case SuperType(_, symbol)    => Seq(symbol)
      case StructuralType(tpe, _)  => typeSymbols(tpe, includeArguments)
    }
  // scalastyle:on cyclomatic.complexity

  // scalastyle:off cyclomatic.complexity
  private def typeArgumentSymbols(typ: Type, includeOwnSymbol: Boolean): Seq[String] =
    typ match {
      case Type.Empty       => Seq.empty
      case WithType(types)  => types.flatMap(typeArgumentSymbols(_, includeOwnSymbol))
      case UnionType(types) => types.flatMap(typeArgumentSymbols(_, includeOwnSymbol))
      case _: ConstantType  =>
        // TODO: Do something better with constant types.
        Seq.empty
      case RepeatedType(tpe)       => typeArgumentSymbols(tpe, includeOwnSymbol)
      case ExistentialType(tpe, _) => typeArgumentSymbols(tpe, includeOwnSymbol)
      case TypeRef(_, symbol, typeArguments) =>
        if (includeOwnSymbol) symbol +: typeArguments.flatMap(typeArgumentSymbols(_, includeOwnSymbol = true))
        else typeArguments.flatMap(typeArgumentSymbols(_, includeOwnSymbol = true))
      case SingleType(_, symbol)   => if (includeOwnSymbol) Seq(symbol) else Seq.empty
      case UniversalType(_, tpe)   => typeArgumentSymbols(tpe, includeOwnSymbol)
      case IntersectionType(types) => types.flatMap(typeArgumentSymbols(_, includeOwnSymbol))
      case ByNameType(tpe)         => typeArgumentSymbols(tpe, includeOwnSymbol)
      case ThisType(symbol)        => if (includeOwnSymbol) Seq(symbol) else Seq.empty
      case AnnotatedType(_, tpe)   => typeArgumentSymbols(tpe, includeOwnSymbol)
      case SuperType(_, symbol)    => if (includeOwnSymbol) Seq(symbol) else Seq.empty
      case StructuralType(tpe, _)  => typeArgumentSymbols(tpe, includeOwnSymbol)
    }
  // scalastyle:on cyclomatic.complexity

  private def optionalScopeTypeParameters(maybeScope: Option[Scope], symbolIndex: SymbolIndex): Seq[TypeParameter] =
    // TODO: What to do about hardlinks?
    maybeScope.map {
      _.symlinks
        .flatMap(symbolIndex.lookup)
        .map(info => (info, info.signature))
        .collect { case (info, typ: TypeSignature) =>
          TypeParameter(info.symbol, typeSymbols(typ.upperBound, includeArguments = false))
        }
    }.getOrElse(Seq.empty)

  private def aggregatorSymbol(symbolInformation: SymbolInformation): String = {
    val symbol = symbolInformation.symbol
    // This is a workaround for https://forum.plantuml.net/13254/unable-to-link-between-fields-in-different-namespaces
    if (symbol.isTerm) symbolOwner(symbol) else symbol
  }

  private def optionalScopeAggregations(
      aggregator: String,
      maybeScope: Option[Scope],
      symbolIndex: SymbolIndex
    ): Seq[Aggregation] =
    maybeScope.map(scopeAggregations(aggregator, _, symbolIndex)).getOrElse(Seq.empty)

  private def scopeAggregations(aggregator: String, scope: Scope, symbolIndex: SymbolIndex): Seq[Aggregation] =
    // TODO: What to do about hardlinks?
    scope.symlinks
      .flatMap(symbolIndex.lookup)
      .flatMap { symbolInformation =>
        symbolInformation.signature match {
          case typ: TypeSignature    => typeSignatureAggregations(aggregator, typ, symbolIndex)
          case value: ValueSignature => typeAggregations(aggregator, value.tpe, symbolIndex)
          case _                     => Seq.empty
        }
      }

  private def typeSignatureAggregations(
      aggregator: String,
      typ: TypeSignature,
      symbolIndex: SymbolIndex
    ): Seq[Aggregation] =
    optionalScopeAggregations(aggregator, typ.typeParameters, symbolIndex) ++
      typeAggregations(aggregator, typ.lowerBound, symbolIndex) ++
      typeAggregations(aggregator, typ.upperBound, symbolIndex)

  private def typeAggregations(aggregator: String, typ: Type, symbolIndex: SymbolIndex): Seq[Aggregation] =
    aggregations(aggregator, typeSymbols(typ, includeArguments = true), symbolIndex)

  private def typeArgumentAggregations(aggregator: String, typ: Type, symbolIndex: SymbolIndex): Seq[Aggregation] =
    aggregations(aggregator, typeArgumentSymbols(typ, includeOwnSymbol = false), symbolIndex)

  private def aggregations(aggregator: String, aggregates: Seq[String], symbolIndex: SymbolIndex): Seq[Aggregation] =
    aggregates.filterNot { symbol =>
      symbol == aggregator ||
      TerminalTypes.contains(symbol) ||
      nonClassSymbol(symbol, symbolIndex)
    }.map(Aggregation(aggregator, _))

  private def nonClassSymbol(symbol: String, symbolIndex: SymbolIndex): Boolean =
    symbol.isLocal || symbolIndex.lookup(symbol).exists { symbolInformation =>
      symbolInformation.signature match {
        case _: ClassSignature => false
        case _                 => true
      }
    }

  // scalastyle:off cyclomatic.complexity
  private def symbolVisibility(symbolInformation: SymbolInformation): Visibility =
    symbolInformation.access match {
      case Access.Empty                                    => Visibility.Public
      case PrivateAccess()                                 => Visibility.Private
      case PrivateThisAccess()                             => Visibility.Private
      case PrivateWithinAccess(symbol) if symbol.isPackage => Visibility.PackagePrivate
      case PrivateWithinAccess(_)                          => Visibility.Private
      case ProtectedAccess()                               => Visibility.Protected
      case ProtectedThisAccess()                           => Visibility.Protected
      case ProtectedWithinAccess(_)                        => Visibility.Protected
      case PublicAccess()                                  => Visibility.Public
    }
  // scalastyle:on cyclomatic.complexity

  // TODO: This should also take into account the synthetics on the text document
  //  although currently they don't seem to be very useful.
  private def isSynthetic(symbol: String, definitionIndex: DefinitionIndex): Boolean =
    definitionIndex.occurrence(symbol).isEmpty

}
