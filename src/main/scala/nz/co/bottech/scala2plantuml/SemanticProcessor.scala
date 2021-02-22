package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.{Annotation => UmlAnnotation}
import org.slf4j.LoggerFactory

import java.io.InputStream
import scala.meta.internal.semanticdb._
import scala.util.Try

object SemanticProcessor {

  private val logger = LoggerFactory.getLogger(classOf[SemanticProcessor.type])

  def processInputStream(inputStream: InputStream): Either[String, List[ClassDiagramElement]] =
    Try {
      val documents = TextDocuments.parseFrom(inputStream).documents.toList
      documents.flatMap(processDocument)
    }.toEither.left.map(_.getMessage)

  private def processDocument(document: TextDocument): List[ClassDiagramElement] = {
    val symbols = document.symbols.toList
    processSymbols(symbols, SymbolIndex.empty).value
  }

  private def processSymbols(symbols: List[SymbolInformation], index: SymbolIndex): Indexed[List[ClassDiagramElement]] =
    symbols.reverse.foldLeft(Indexed(List.empty[ClassDiagramElement], index)) {
      case (acc, symbol) =>
        logger.trace(symbolInformationString(symbol))
        symbol.signature match {
          case Signature.Empty         => acc
          case value: ValueSignature   => acc
          case clazz: ClassSignature   => processClass(symbol, clazz, acc.index).map(_ ++ acc.value)
          case method: MethodSignature => acc
          case typ: TypeSignature      => acc
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
      clazz: ClassSignature,
      index: SymbolIndex
    ): Indexed[List[ClassDiagramElement]] = {
    val Indexed(isAnnotation, updatedIndex) = subTypeOf(symbolInformation, "scala/annotation/Annotation#", index)
    val result =
      if (isAnnotation) List(UmlAnnotation(symbolInformation.displayName))
      else if (isAbstract(symbolInformation)) List(AbstractClass(symbolInformation.displayName))
      else List(ConcreteClass(symbolInformation.displayName))
    Indexed(result, updatedIndex)
  }

  private def subTypeOf(symbolInformation: SymbolInformation, parent: String, index: SymbolIndex): Indexed[Boolean] = {
    // TODO: Do we need to cache the type hierarchy?
    val indexedHierarchy = TypeHierarchy.create(symbolInformation, index)
    indexedHierarchy.map(_.subTypeOf(parent))
  }

  def isAbstract(symbolInformation: SymbolInformation): Boolean =
    hasProperty(symbolInformation, SymbolInformation.Property.ABSTRACT)

  private def hasProperty(symbolInformation: SymbolInformation, property: SymbolInformation.Property): Boolean =
    (symbolInformation.properties & property.value) == property.value
}
