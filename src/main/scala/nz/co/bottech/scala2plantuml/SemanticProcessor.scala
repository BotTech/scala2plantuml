package nz.co.bottech.scala2plantuml

import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.collection.immutable.HashSet
import scala.meta.internal.semanticdb.Scala._
import scala.meta.internal.semanticdb._
import scala.meta.internal.symtab.SymbolTable

// TODO: Rename this.
private[scala2plantuml] object SemanticProcessor {

  private val logger = LoggerFactory.getLogger(classOf[SemanticProcessor.type])

  def processSymbol(
      symbol: String,
      ignore: String => Boolean,
      symbolTable: SymbolTable,
      typeIndex: TypeIndex,
      definitionIndex: DefinitionIndex
    ): Seq[ClassDiagramElement] = {
    @tailrec
    def loop(
        remaining: Vector[String],
        seen: HashSet[String],
        acc: Vector[ClassDiagramElement]
      ): Vector[ClassDiagramElement] =
      remaining match {
        case current +: tail if seen.contains(current) || ignore(current) =>
          loop(tail, seen, acc)
        case current +: tail =>
          symbolTable.info(current) match {
            case Some(symbolInformation: SymbolInformation) =>
              logger.trace(symbolInformationString(symbolInformation))
              val elements = symbolElements(symbolInformation, typeIndex, definitionIndex)
              // Traverse breadth-first so that we process a full symbol before moving onto the next.
              val nextSymbols = tail ++ symbolReferences(symbolInformation)
              val nextSeen    = seen + current
              loop(nextSymbols, nextSeen, elements ++ acc)
            case None =>
              if (!scalaStdLibSymbol(current)) logger.warn(s"Missing symbol for $current")
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
      typeIndex: TypeIndex,
      definitionIndex: DefinitionIndex
    ): Vector[ClassDiagramElement] =
    symbolInformation.signature match {
      case Signature.Empty    => Vector.empty
      case _: ValueSignature  => Vector.empty
      case _: ClassSignature  => Vector(classElement(symbolInformation, typeIndex))
      case _: MethodSignature => Vector(methodElement(symbolInformation, definitionIndex))
      case _: TypeSignature   => Vector.empty
    }

  private def classElement(
      symbolInformation: SymbolInformation,
      typeIndex: TypeIndex
    ): ClassDiagramElement = {
    import symbolInformation.{displayName, symbol}
    if (isTrait(symbolInformation))
      UmlInterface(displayName, symbol)
    else if (isAnnotation(symbolInformation, typeIndex))
      UmlAnnotation(displayName, symbol, isObject = isObject(symbolInformation))
    else if (isEnum(symbolInformation, typeIndex))
      UmlEnum(displayName, symbol, isObject = isObject(symbolInformation))
    else if (isAbstract(symbolInformation))
      UmlAbstractClass(displayName, symbol)
    else
      UmlClass(displayName, symbol, isObject = isObject(symbolInformation))
  }

  private def methodElement(
      symbolInformation: SymbolInformation,
      definitionIndex: DefinitionIndex
    ): ClassDiagramElement = {
    import symbolInformation.{displayName, symbol}
    val visibility = symbolVisibility(symbolInformation)
    if (isField(symbolInformation))
      UmlField(displayName, symbol, visibility)
    else
      UmlMethod(
        displayName,
        symbol,
        visibility,
        isConstructor(symbolInformation),
        isSynthetic(symbolInformation.symbol, definitionIndex)
      )
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
      case Type.Empty                         => Seq.empty
      case WithType(types)                    => types.flatMap(typeReferences)
      case UnionType(types)                   => types.flatMap(typeReferences)
      case _: ConstantType                    => Seq.empty
      case RepeatedType(tpe)                  => typeReferences(tpe)
      case ExistentialType(tpe, declarations) => typeReferences(tpe) ++ optionalScopeReferences(declarations)
      case TypeRef(prefix, symbol, typeArguments) =>
        symbol +: typeReferences(prefix) ++: typeArguments.flatMap(typeReferences)
      case SingleType(prefix, symbol)         => symbol +: typeReferences(prefix)
      case UniversalType(typeParameters, tpe) => typeReferences(tpe) ++ optionalScopeReferences(typeParameters)
      case IntersectionType(types)            => types.flatMap(typeReferences)
      case ByNameType(tpe)                    => typeReferences(tpe)
      case ThisType(symbol)                   => Seq(symbol)
      case AnnotatedType(annotations, tpe) =>
        typeReferences(tpe) ++ annotations.flatMap(annotation => typeReferences(annotation.tpe))
      case SuperType(prefix, symbol)         => symbol +: typeReferences(prefix)
      case StructuralType(tpe, declarations) => typeReferences(tpe) ++ optionalScopeReferences(declarations)
    }

  private def symbolVisibility(symbolInformation: SymbolInformation): UmlVisibility =
    symbolInformation.access match {
      case Access.Empty                                    => UmlVisibility.Public
      case PrivateAccess()                                 => UmlVisibility.Private
      case PrivateThisAccess()                             => UmlVisibility.Private
      case PrivateWithinAccess(symbol) if symbol.isPackage => UmlVisibility.PackagePrivate
      case PrivateWithinAccess(_)                          => UmlVisibility.Private
      case ProtectedAccess()                               => UmlVisibility.Protected
      case ProtectedThisAccess()                           => UmlVisibility.Protected
      case ProtectedWithinAccess(_)                        => UmlVisibility.Protected
      case PublicAccess()                                  => UmlVisibility.Public
    }

  private def isAnnotation(symbolInformation: SymbolInformation, typeIndex: TypeIndex): Boolean =
    subTypeOf(symbolInformation, "scala/annotation/Annotation#", typeIndex)

  private def isEnum(symbolInformation: SymbolInformation, typeIndex: TypeIndex): Boolean =
    subTypeOf(symbolInformation, "scala/Enumeration#", typeIndex) ||
      subTypeOf(symbolInformation, "java/lang/Enum#", typeIndex)

  private def subTypeOf(symbolInformation: SymbolInformation, parent: String, typeIndex: TypeIndex): Boolean = {
    val hierarchy = typeIndex.hierarchy(symbolInformation)
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
