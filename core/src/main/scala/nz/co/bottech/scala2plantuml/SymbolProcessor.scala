package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.AdditionalSymbolInformation._
import nz.co.bottech.scala2plantuml.AggregationProcessor.symbolAggregations
import nz.co.bottech.scala2plantuml.ClassDiagramElement.{Annotation => UMLAnnotation, Type => _, _}
import nz.co.bottech.scala2plantuml.Collections.IterableOnce
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.collection.immutable.HashSet
import scala.meta.internal.semanticdb.Scala._
import scala.meta.internal.semanticdb._

private[scala2plantuml] object SymbolProcessor {

  private val Logger = LoggerFactory.getLogger(getClass.getName.dropRight(1))

  // TODO: Perhaps we should return a different type than the types used for rendering.
  def processSymbol(
      symbol: String,
      maxLevel: Option[Int],
      symbolIndex: SymbolIndex,
      typeIndex: TypeIndex,
      definitionIndex: DefinitionIndex
    ): Seq[ClassDiagramElement] = {
    def skip(symbol: String, seen: HashSet[String]): Boolean =
      seen.contains(symbol) || !symbolIndex.indexOf(symbol)
    @tailrec
    def loop(
        level: Int,
        currentLevelSymbols: Vector[String],
        nextLevelSymbols: Vector[String],
        seen: HashSet[String],
        acc: Vector[ClassDiagramElement]
      ): Vector[ClassDiagramElement] = {
      checkIfInterrupted()
      currentLevelSymbols match {
        case current +: tail if skip(current, seen) =>
          loop(level, tail, nextLevelSymbols, seen, acc)
        case current +: tail =>
          symbolIndex.lookup(current) match {
            case Some(symbolInformation: SymbolInformation) =>
              Logger.trace(symbolInformationString(symbolInformation))
              val maxLevelReached = maxLevel.exists(_ <= level)
              val elements =
                symbolElements(symbolInformation, maxLevelReached, symbolIndex, typeIndex, definitionIndex)
              val members = memberReferences(symbolInformation)
              val references =
                if (maxLevelReached) Seq.empty
                else symbolReferences(symbolInformation)
              val nextCurrentLevelSymbols = members ++: tail
              val nextNextLevelSymbols    = nextLevelSymbols ++ references
              val nextSeen                = seen + current
              loop(level, nextCurrentLevelSymbols, nextNextLevelSymbols, nextSeen, acc ++ elements)
            case None =>
              Logger.warn(s"Missing symbol: $current")
              loop(level, tail, nextLevelSymbols, seen, acc)
          }
        case _ if nextLevelSymbols.nonEmpty =>
          loop(level + 1, nextLevelSymbols, Vector.empty, seen, acc)
        case _ => acc
      }
    }

    loop(1, Vector(symbol), Vector.empty, HashSet.empty, Vector.empty)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  private def checkIfInterrupted(): Unit =
    if (Thread.currentThread().isInterrupted)
      throw new InterruptedException("Interrupted while processing symbol.")

  private def symbolElements(
      symbolInformation: SymbolInformation,
      maxLevelReached: Boolean,
      symbolIndex: SymbolIndex,
      typeIndex: TypeIndex,
      definitionIndex: DefinitionIndex
    ): IterableOnce[ClassDiagramElement] = {
    val element = symbolDefinition(symbolInformation, maxLevelReached, symbolIndex, typeIndex, definitionIndex)
    if (maxLevelReached) element
    else element ++: symbolAggregations(symbolInformation, symbolIndex)
  }

  private def symbolInformationString(symbol: SymbolInformation): String =
    s"""SymbolInformation(
       |  language: ${symbol.language.toString}
       |  symbol: ${symbol.symbol}
       |  kind: ${symbol.kind.toString}
       |  display_name: ${symbol.displayName}
       |  signature: ${symbol.signature.toString}
       |)""".stripMargin

  private def symbolDefinition(
      symbolInformation: SymbolInformation,
      excludeParents: Boolean,
      symbolIndex: SymbolIndex,
      typeIndex: TypeIndex,
      definitionIndex: DefinitionIndex
    ): Option[Definition] =
    symbolInformation.signature match {
      case Signature.Empty         => None
      case clazz: ClassSignature   => Some(classType(symbolInformation, clazz, excludeParents, symbolIndex, typeIndex))
      case method: MethodSignature => Some(methodMember(symbolInformation, method, symbolIndex, definitionIndex))
      case _: TypeSignature        => None // We can't do much with this because we don't know the aggregator.
      case _: ValueSignature       => None // Same here.
    }

  private def classType(
      symbolInformation: SymbolInformation,
      clazz: ClassSignature,
      excludeParents: Boolean,
      symbolIndex: SymbolIndex,
      typeIndex: TypeIndex
    ): ClassDiagramElement.Type = {
    import symbolInformation.{displayName, symbol}
    val parentSymbols =
      if (excludeParents) Seq.empty
      else
        clazz.parents
          .flatMap(typeSymbols(_, includeArguments = false))
          .filter(symbolIndex.indexOf)
    val typeParameters = optionalScopeTypeParameters(clazz.typeParameters, symbolIndex)
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
  }

  private def methodMember(
      symbolInformation: SymbolInformation,
      method: MethodSignature,
      symbolIndex: SymbolIndex,
      definitionIndex: DefinitionIndex
    ): Member = {
    import symbolInformation.{displayName, symbol}
    val visibility = symbolVisibility(symbolInformation)
    if (isField(symbolInformation)) {
      Field(displayName, symbol, visibility, isAbstract(symbolInformation))
    } else {
      val typeParameters = optionalScopeTypeParameters(method.typeParameters, symbolIndex)
      Method(
        displayName,
        symbol,
        visibility,
        isConstructor(symbolInformation),
        isSynthetic(symbolInformation.symbol, definitionIndex),
        isAbstract(symbolInformation),
        typeParameters
      )
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

  private def memberReferences(symbolInformation: SymbolInformation): Seq[String] =
    symbolInformation.signature match {
      case clazz: ClassSignature => classMemberReferences(clazz)
      case _                     => Seq.empty
    }

  private def valueReferences(value: ValueSignature): Seq[String] =
    typeReferences(value.tpe)

  // This intentionally doesn't include the declarations as we need to handle them separately.
  private def classReferences(clazz: ClassSignature): Seq[String] =
    typeReferences(clazz.self) ++
      clazz.parents.flatMap(typeReferences) ++
      optionalScopeReferences(clazz.typeParameters)

  private def classMemberReferences(clazz: ClassSignature): Seq[String] =
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
  def typeSymbols(typ: Type, includeArguments: Boolean): Seq[String] =
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
