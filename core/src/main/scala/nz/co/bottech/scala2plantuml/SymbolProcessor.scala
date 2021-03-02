package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramElement.{Annotation => UMLAnnotation, Type => _, _}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.collection.immutable.HashSet
import scala.meta.internal.semanticdb.Scala._
import scala.meta.internal.semanticdb._
import scala.meta.internal.symtab.SymbolTable

private[scala2plantuml] object SymbolProcessor {

  private val logger = LoggerFactory.getLogger(getClass.getName.dropRight(1))

  // TODO: Perhaps we should return a different type than the types used for rendering.
  def processSymbol(
      symbol: String,
      ignore: String => Boolean,
      symbolTable: SymbolTable,
      typeIndex: TypeIndex,
      definitionIndex: DefinitionIndex
    ): Seq[ClassDiagramElement] = {
    def skip(symbol: String, seen: HashSet[String]): Boolean =
      // Only global symbols can be found in the symbol table.
      !symbol.isGlobal ||
        seen.contains(symbol) ||
        ignore(symbol)
    @tailrec
    def loop(
        remaining: Vector[String],
        seen: HashSet[String],
        acc: Vector[ClassDiagramElement]
      ): Vector[ClassDiagramElement] =
      remaining match {
        case current +: tail if skip(current, seen) =>
          loop(tail, seen, acc)
        case current +: tail =>
          symbolTable.info(current) match {
            case Some(symbolInformation: SymbolInformation) =>
              logger.trace(symbolInformationString(symbolInformation))
              val elements = symbolElements(symbolInformation, ignore, symbolTable, typeIndex, definitionIndex)
              // Traverse breadth-first so that we process a full symbol before moving onto the next.
              val nextSymbols = tail ++ symbolReferences(symbolInformation)
              val nextSeen    = seen + current
              loop(nextSymbols, nextSeen, elements ++: acc)
            case None =>
              logger.warn(s"Missing symbol: $current")
              loop(remaining.init, seen, acc)
          }
        case _ => acc
      }

    loop(Vector(symbol), HashSet.empty, Vector.empty)
  }

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
      ignore: String => Boolean,
      symbolTable: SymbolTable,
      typeIndex: TypeIndex,
      definitionIndex: DefinitionIndex
    ): Seq[ClassDiagramElement] =
    symbolInformation.signature match {
      case Signature.Empty         => Seq.empty
      case clazz: ClassSignature   => classElement(symbolInformation, clazz, ignore, symbolTable, typeIndex)
      case method: MethodSignature => methodElements(symbolInformation, method, symbolTable, definitionIndex)
      case _: TypeSignature        => Seq.empty // We can't do much with this because we don't know the aggregator.
      case _: ValueSignature       => Seq.empty // Same here.
    }

  // TODO: Why ignore here?
  private def classElement(
      symbolInformation: SymbolInformation,
      clazz: ClassSignature,
      ignore: String => Boolean,
      symbolTable: SymbolTable,
      typeIndex: TypeIndex
    ): Seq[ClassDiagramElement] = {
    import symbolInformation.{displayName, symbol}
    val parentSymbols  = clazz.parents.flatMap(typeSymbols)
    val typeParameters = optionalScopeTypeParameters(clazz.typeParameters, symbolTable)
    val element =
      if (isTrait(symbolInformation))
        Interface(displayName, symbol, parentSymbols, typeParameters)
      else if (isAnnotation(symbolInformation, ignore, typeIndex))
        UMLAnnotation(displayName, symbol, isObject(symbolInformation), parentSymbols, typeParameters)
      else if (isEnum(symbolInformation, ignore, typeIndex))
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
    val typeParameterAggregations = optionalScopeAggregations(aggregator, clazz.typeParameters, symbolTable)
    element +: typeParameterAggregations
  }

  private def methodElements(
      symbolInformation: SymbolInformation,
      method: MethodSignature,
      symbolTable: SymbolTable,
      definitionIndex: DefinitionIndex
    ): Seq[ClassDiagramElement] = {
    import symbolInformation.{displayName, symbol}
    val visibility = symbolVisibility(symbolInformation)
    val aggregator = aggregatorSymbol(symbolInformation)
    if (isField(symbolInformation)) {
      val element                = Field(displayName, symbol, visibility, isAbstract(symbolInformation))
      val returnTypeAggregations = typeAggregations(aggregator, method.returnType)
      element +: returnTypeAggregations
    } else {
      val typeParameters = optionalScopeTypeParameters(method.typeParameters, symbolTable)
      val element = Method(
        displayName,
        symbol,
        visibility,
        isConstructor(symbolInformation),
        isSynthetic(symbolInformation.symbol, definitionIndex),
        isAbstract(symbolInformation),
        typeParameters
      )
      val typeParameterAggregations = optionalScopeAggregations(aggregator, method.typeParameters, symbolTable)
      val parameterAggregations     = method.parameterLists.flatMap(scopeAggregations(aggregator, _, symbolTable))
      val returnTypeAggregations    = typeAggregations(aggregator, method.returnType)
      element +: (typeParameterAggregations ++ parameterAggregations ++ returnTypeAggregations)
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

  private def typeReferences(typ: Type): Seq[String] =
    typ match {
      case Type.Empty       => Seq.empty
      case WithType(types)  => types.flatMap(typeReferences)
      case UnionType(types) => types.flatMap(typeReferences)
      case _: ConstantType  =>
        // TODO: Need to do something better here.
        Seq.empty
      case RepeatedType(tpe)                  => typeReferences(tpe)
      case ExistentialType(tpe, declarations) =>
        // TODO: Existential types ought to be shown as their own type.
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

  private def typeSymbols(typ: Type): Seq[String] =
    typ match {
      case Type.Empty       => Seq.empty
      case WithType(types)  => types.flatMap(typeSymbols)
      case UnionType(types) => types.flatMap(typeSymbols)
      case _: ConstantType  =>
        // TODO: Need to do something better here.
        Seq.empty
      case RepeatedType(tpe)       => typeSymbols(tpe)
      case ExistentialType(tpe, _) => typeSymbols(tpe)
      case TypeRef(_, symbol, _)   => Seq(symbol)
      case SingleType(_, symbol)   => Seq(symbol)
      case UniversalType(_, tpe)   => typeSymbols(tpe)
      case IntersectionType(types) => types.flatMap(typeSymbols)
      case ByNameType(tpe)         => typeSymbols(tpe)
      case ThisType(symbol)        => Seq(symbol)
      case AnnotatedType(_, tpe)   => typeSymbols(tpe)
      case SuperType(_, symbol)    => Seq(symbol)
      case StructuralType(tpe, _)  => typeSymbols(tpe)
    }

  private def optionalScopeTypeParameters(maybeScope: Option[Scope], symbolTable: SymbolTable): Seq[TypeParameter] =
    // TODO: What to do about hardlinks?
    maybeScope.map {
      _.symlinks
        .flatMap(symbolTable.info)
        .map(info => (info, info.signature))
        .collect {
          case (info, typ: TypeSignature) => TypeParameter(info.symbol, typeSymbols(typ.upperBound))
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
      symbolTable: SymbolTable
    ): Seq[Aggregation] =
    maybeScope.map(scopeAggregations(aggregator, _, symbolTable)).getOrElse(Seq.empty)

  private def scopeAggregations(
      aggregator: String,
      scope: Scope,
      symbolTable: SymbolTable
    ): Seq[Aggregation] =
    // TODO: What to do about hardlinks?
    scope.symlinks
      .flatMap(symbolTable.info)
      .map(_.signature)
      .flatMap {
        case typ: TypeSignature    => typeSignatureAggregations(aggregator, typ)
        case value: ValueSignature => typeAggregations(aggregator, value.tpe)
        case _                     => Seq.empty
      }

  private def typeSignatureAggregations(aggregator: String, typ: TypeSignature): Seq[Aggregation] =
    typeParameterAggregations(aggregator, typ.typeParameters) ++
      typeAggregations(aggregator, typ.lowerBound) ++
      typeAggregations(aggregator, typ.upperBound)

  private def typeParameterAggregations(aggregator: String, maybeScope: Option[Scope]): Seq[Aggregation] =
    optionalScopeReferences(maybeScope).filterNot(_ == aggregator).map(Aggregation(aggregator, _))

  private def typeAggregations(aggregator: String, typ: Type): Seq[Aggregation] =
    typeSymbols(typ).filterNot(_ == aggregator).map(Aggregation(aggregator, _))

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

  private def isAnnotation(
      symbolInformation: SymbolInformation,
      ignore: String => Boolean,
      typeIndex: TypeIndex
    ): Boolean =
    subTypeOf(symbolInformation, "scala/annotation/Annotation#", ignore, typeIndex)

  private def isEnum(symbolInformation: SymbolInformation, ignore: String => Boolean, typeIndex: TypeIndex): Boolean =
    subTypeOf(symbolInformation, "scala/Enumeration#", ignore, typeIndex) ||
      subTypeOf(symbolInformation, "java/lang/Enum#", ignore, typeIndex)

  private def subTypeOf(
      symbolInformation: SymbolInformation,
      parent: String,
      ignore: String => Boolean,
      typeIndex: TypeIndex
    ): Boolean = {
    val hierarchy = typeIndex.hierarchy(symbolInformation, ignore)
    hierarchy.subTypeOf(parent)
  }

  // TODO: This should also take into account the synthetics on the text document
  //  although currently they don't seem to be very useful.
  private def isSynthetic(symbol: String, definitionIndex: DefinitionIndex): Boolean =
    definitionIndex.occurrence(symbol).isEmpty

  private def isObject(symbolInformation: SymbolInformation): Boolean =
    symbolInformation.kind == SymbolInformation.Kind.OBJECT

  private def isTrait(symbolInformation: SymbolInformation): Boolean =
    symbolInformation.kind == SymbolInformation.Kind.TRAIT

  private def isConstructor(symbolInformation: SymbolInformation): Boolean =
    symbolInformation.kind == SymbolInformation.Kind.CONSTRUCTOR

  private def isAbstract(symbolInformation: SymbolInformation): Boolean =
    hasProperty(symbolInformation, SymbolInformation.Property.ABSTRACT)

  private def isField(symbolInformation: SymbolInformation): Boolean =
    isVal(symbolInformation) || isVar(symbolInformation)

  private def isVal(symbolInformation: SymbolInformation): Boolean =
    hasProperty(symbolInformation, SymbolInformation.Property.VAL)

  private def isVar(symbolInformation: SymbolInformation): Boolean =
    hasProperty(symbolInformation, SymbolInformation.Property.VAR)

  private def hasProperty(symbolInformation: SymbolInformation, property: SymbolInformation.Property): Boolean =
    (symbolInformation.properties & property.value) == property.value
}
