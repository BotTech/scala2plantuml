package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.{Annotation => UmlAnnotation}
import org.slf4j.LoggerFactory

import scala.meta.internal.semanticdb.Scala._
import scala.meta.internal.semanticdb._
import scala.meta.internal.symtab.SymbolTable

object SemanticProcessor {

  private val logger = LoggerFactory.getLogger(classOf[SemanticProcessor.type])

  def processDocument(document: TextDocument, symbolTable: SymbolTable): List[ClassDiagramElement] = {
    val symbols = document.symbols.toList
    processSymbols(symbols, symbolTable)
  }

  private def processSymbols(symbols: List[SymbolInformation], symbolTable: SymbolTable): List[ClassDiagramElement] =
    symbols.flatMap { symbol =>
      logger.trace(symbolInformationString(symbol))
      symbol.signature match {
        case Signature.Empty    => None
        case _: ValueSignature  => None
        case _: ClassSignature  => Some(processClass(symbol, symbolTable))
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
      symbolTable: SymbolTable
    ): ClassDiagramElement = {
    val displayName = symbolInformation.displayName
    val fullName    = symbolFullName(symbolInformation.symbol)
    if (isAnnotation(symbolInformation, symbolTable))
      UmlAnnotation(displayName, fullName, isObject = isObject(symbolInformation))
    else if (isAbstract(symbolInformation)) AbstractClass(displayName, fullName)
    else ConcreteClass(displayName, fullName, isObject = isObject(symbolInformation))
  }

  private def symbolFullName(symbol: String): String =
    if (symbol.isGlobal) symbol.replace('/', '.').dropRight(1)
    else symbol.replace('/', '.')

  private def isAnnotation(symbolInformation: SymbolInformation, symbolTable: SymbolTable): Boolean =
    subTypeOf(symbolInformation, "scala/annotation/Annotation#", symbolTable)

  private def subTypeOf(symbolInformation: SymbolInformation, parent: String, symbolTable: SymbolTable): Boolean = {
    // TODO: Do we need to cache the type hierarchy? YES!
    val hierarchy = TypeHierarchy.create(symbolInformation, symbolTable)
    hierarchy.subTypeOf(parent)
  }

  private def isAbstract(symbolInformation: SymbolInformation): Boolean =
    hasProperty(symbolInformation, SymbolInformation.Property.ABSTRACT)

  private def hasProperty(symbolInformation: SymbolInformation, property: SymbolInformation.Property): Boolean =
    (symbolInformation.properties & property.value) == property.value

  private def isObject(symbolInformation: SymbolInformation): Boolean =
    symbolInformation.kind == SymbolInformation.Kind.OBJECT
}
