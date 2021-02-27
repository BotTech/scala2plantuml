package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramElement.{Annotation => UMLAnnotation, _}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.collection.immutable.HashSet
import scala.meta.internal.semanticdb.Scala._
import scala.meta.internal.semanticdb._
import scala.meta.internal.symtab.SymbolTable

private[scala2plantuml] object SymbolProcessor {

  private val logger = LoggerFactory.getLogger(getClass.getName.dropRight(1))

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
              val elements = symbolElements(symbolInformation, ignore, typeIndex, definitionIndex)
              // Traverse breadth-first so that we process a full symbol before moving onto the next.
              val nextSymbols = tail ++ symbolReferences(symbolInformation)
              val nextSeen    = seen + current
              loop(nextSymbols, nextSeen, elements ++ acc)
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
      typeIndex: TypeIndex,
      definitionIndex: DefinitionIndex
    ): Vector[ClassDiagramElement] =
    symbolInformation.signature match {
      case Signature.Empty    => Vector.empty
      case _: ValueSignature  => Vector.empty
      case _: ClassSignature  => Vector(classElement(symbolInformation, ignore, typeIndex))
      case _: MethodSignature => Vector(methodElement(symbolInformation, definitionIndex))
      case _: TypeSignature   => Vector.empty
    }

  private def classElement(
      symbolInformation: SymbolInformation,
      ignore: String => Boolean,
      typeIndex: TypeIndex
    ): ClassDiagramElement = {
    import symbolInformation.{displayName, symbol}
    if (isTrait(symbolInformation))
      Interface(displayName, symbol)
    else if (isAnnotation(symbolInformation, ignore, typeIndex))
      UMLAnnotation(displayName, symbol, isObject = isObject(symbolInformation))
    else if (isEnum(symbolInformation, ignore, typeIndex))
      Enum(displayName, symbol, isObject = isObject(symbolInformation))
    else
      Class(displayName, symbol, isObject(symbolInformation), isAbstract(symbolInformation))
  }

  private def methodElement(
      symbolInformation: SymbolInformation,
      definitionIndex: DefinitionIndex
    ): ClassDiagramElement = {
    import symbolInformation.{displayName, symbol}
    val visibility = symbolVisibility(symbolInformation)
    if (isField(symbolInformation))
      Field(displayName, symbol, visibility, isAbstract(symbolInformation))
    else
      Method(
        displayName,
        symbol,
        visibility,
        isConstructor(symbolInformation),
        isSynthetic(symbolInformation.symbol, definitionIndex),
        isAbstract(symbolInformation)
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
