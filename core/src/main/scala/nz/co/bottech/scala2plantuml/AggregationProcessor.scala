package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.AdditionalSymbolInformation.isField
import nz.co.bottech.scala2plantuml.ClassDiagramElement.Aggregation
import nz.co.bottech.scala2plantuml.SymbolProcessor.typeSymbols

import scala.meta.internal.semanticdb.Scala._
import scala.meta.internal.semanticdb._

private[scala2plantuml] object AggregationProcessor {

  private val TerminalTypes = Set("scala/Any#", "scala/Nothing#")

  def symbolAggregations(
      symbolInformation: SymbolInformation,
      symbolIndex: SymbolIndex
    ): Seq[ClassDiagramElement] =
    symbolInformation.signature match {
      case Signature.Empty         => Seq.empty
      case clazz: ClassSignature   => classAggregations(symbolInformation, clazz, symbolIndex)
      case method: MethodSignature => methodAggregations(symbolInformation, method, symbolIndex)
      case _: TypeSignature        => Seq.empty // We can't do much with this because we don't know the aggregator.
      case _: ValueSignature       => Seq.empty // Same here.
    }

  private def classAggregations(
      symbolInformation: SymbolInformation,
      clazz: ClassSignature,
      symbolIndex: SymbolIndex
    ): Seq[ClassDiagramElement] = {
    val aggregator                = aggregatorSymbol(symbolInformation)
    val typeParameterAggregations = optionalScopeAggregations(aggregator, clazz.typeParameters, symbolIndex)
    val parentAggregations        = clazz.parents.flatMap(typeArgumentAggregations(aggregator, _, symbolIndex))
    typeParameterAggregations ++: parentAggregations
  }

  private def methodAggregations(
      symbolInformation: SymbolInformation,
      method: MethodSignature,
      symbolIndex: SymbolIndex
    ): Seq[ClassDiagramElement] = {
    val aggregator = aggregatorSymbol(symbolInformation)
    if (isField(symbolInformation)) {
      typeAggregations(aggregator, method.returnType, symbolIndex)
    } else {
      val typeParameterAggregations = optionalScopeAggregations(aggregator, method.typeParameters, symbolIndex)
      val parameterAggregations     = method.parameterLists.flatMap(scopeAggregations(aggregator, _, symbolIndex))
      val returnTypeAggregations: Seq[ClassDiagramElement] =
        typeAggregations(aggregator, method.returnType, symbolIndex)
      typeParameterAggregations ++: parameterAggregations ++: returnTypeAggregations
    }
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
      !symbolIndex.indexOf(symbol) ||
      nonClassSymbol(symbol, symbolIndex)
    }.map(Aggregation(aggregator, _))

  private def aggregatorSymbol(symbolInformation: SymbolInformation): String = {
    val symbol = symbolInformation.symbol
    // This is a workaround for https://forum.plantuml.net/13254/unable-to-link-between-fields-in-different-namespaces
    if (symbol.isTerm) symbolOwner(symbol) else symbol
  }

  private def nonClassSymbol(symbol: String, symbolIndex: SymbolIndex): Boolean =
    symbol.isLocal || symbolIndex.lookup(symbol).exists { symbolInformation =>
      symbolInformation.signature match {
        case _: ClassSignature => false
        case _                 => true
      }
    }

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
}
