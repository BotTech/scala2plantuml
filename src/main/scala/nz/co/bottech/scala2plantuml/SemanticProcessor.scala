package nz.co.bottech.scala2plantuml

import org.slf4j.LoggerFactory

import scala.meta.internal.semanticdb.Scala._
import scala.meta.internal.semanticdb._

object SemanticProcessor {

  private val logger = LoggerFactory.getLogger(classOf[SemanticProcessor.type])

  def processDocument(
      document: TextDocument,
      typeIndex: TypeIndex,
      definitionIndex: DefinitionIndex
    ): List[ClassDiagramElement] = {
    val symbols = document.symbols.toList
    processSymbols(symbols, typeIndex, definitionIndex)
  }

  private def processSymbols(
      symbols: List[SymbolInformation],
      typeIndex: TypeIndex,
      definitionIndex: DefinitionIndex
    ): List[ClassDiagramElement] =
    symbols.flatMap { symbol =>
      logger.trace(symbolInformationString(symbol))
      symbol.signature match {
        case Signature.Empty    => None
        case _: ValueSignature  => None
        case _: ClassSignature  => Some(processClass(symbol, typeIndex))
        case _: MethodSignature => Some(processMethod(symbol, definitionIndex))
        case _: TypeSignature   => None
      }
    }

  private def symbolInformationString(symbol: SymbolInformation): String =
    s"""SymbolInformation(
       |  language: ${symbol.language}
       |  symbol: ${symbol.symbol}
       |  kind: ${symbol.kind}
       |  display_name: ${symbol.displayName}
       |  signature: ${symbol.signature}
       |)""".stripMargin

  private def processClass(
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

  private def processMethod(
      symbolInformation: SymbolInformation,
      definitionIndex: DefinitionIndex
    ): ClassDiagramElement = {
    import symbolInformation.{displayName, symbol}
    val visibility  = symbolVisibility(symbolInformation)
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
