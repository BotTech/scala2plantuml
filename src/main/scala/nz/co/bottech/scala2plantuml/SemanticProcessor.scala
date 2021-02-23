package nz.co.bottech.scala2plantuml

import org.slf4j.LoggerFactory

import scala.meta.internal.semanticdb.Scala._
import scala.meta.internal.semanticdb._

object SemanticProcessor {

  private val logger = LoggerFactory.getLogger(classOf[SemanticProcessor.type])

  def processDocument(document: TextDocument, index: TypeIndex): List[ClassDiagramElement] = {
    val symbols = document.symbols.toList
    processSymbols(symbols, index)
  }

  private def processSymbols(symbols: List[SymbolInformation], index: TypeIndex): List[ClassDiagramElement] =
    symbols.flatMap { symbol =>
      logger.trace(symbolInformationString(symbol))
      symbol.signature match {
        case Signature.Empty    => None
        case _: ValueSignature  => None
        case _: ClassSignature  => Some(processClass(symbol, index))
        case _: MethodSignature => None
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
      index: TypeIndex
    ): ClassDiagramElement = {
    val displayName = symbolInformation.displayName
    val fullName    = symbolFullName(symbolInformation.symbol)
    if (isTrait(symbolInformation))
      UmlInterface(displayName, fullName)
    else if (isAnnotation(symbolInformation, index))
      UmlAnnotation(displayName, fullName, isObject = isObject(symbolInformation))
    else if (isEnum(symbolInformation, index))
      UmlEnum(displayName, fullName, isObject = isObject(symbolInformation))
    else if (isAbstract(symbolInformation))
      UmlAbstractClass(displayName, fullName)
    else
      UmlClass(displayName, fullName, isObject = isObject(symbolInformation))
  }

  private def symbolFullName(symbol: String): String =
    if (symbol.isGlobal) symbol.replace('/', '.').dropRight(1)
    else symbol.replace('/', '.')

  private def isAnnotation(symbolInformation: SymbolInformation, index: TypeIndex): Boolean =
    subTypeOf(symbolInformation, "scala/annotation/Annotation#", index)

  private def isEnum(symbolInformation: SymbolInformation, index: TypeIndex): Boolean =
    subTypeOf(symbolInformation, "scala/Enumeration#", index) ||
      subTypeOf(symbolInformation, "java/lang/Enum#", index)

  private def subTypeOf(symbolInformation: SymbolInformation, parent: String, index: TypeIndex): Boolean = {
    val hierarchy = index.getHierarchy(symbolInformation)
    hierarchy.subTypeOf(parent)
  }

  private def isAbstract(symbolInformation: SymbolInformation): Boolean =
    hasProperty(symbolInformation, SymbolInformation.Property.ABSTRACT)

  private def hasProperty(symbolInformation: SymbolInformation, property: SymbolInformation.Property): Boolean =
    (symbolInformation.properties & property.value) == property.value

  private def isObject(symbolInformation: SymbolInformation): Boolean =
    symbolInformation.kind == SymbolInformation.Kind.OBJECT

  private def isTrait(symbolInformation: SymbolInformation): Boolean =
    symbolInformation.kind == SymbolInformation.Kind.TRAIT
}
