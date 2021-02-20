package nz.co.bottech.scala2plantuml

import org.slf4j.LoggerFactory

import java.io.InputStream
import scala.meta.internal.semanticdb._
import scala.util.Try

object SemanticProcessor {

  private val logger = LoggerFactory.getLogger(classOf[SemanticProcessor.type])

  def processInputStream(inputStream: InputStream): Either[String, List[ClassDiagramElement]] = {
    Try {
      val documents = TextDocuments.parseFrom(inputStream).documents.toList
      documents.flatMap(processDocument)
    }.toEither.left.map(_.getMessage)
  }

  private def processDocument(document: TextDocument): List[ClassDiagramElement] = {
    document.symbols.toList.flatMap { symbol =>
      logger.trace(
        s"""SymbolInformation(
           |  class: ${symbol.getClass}
           |  language: ${symbol.language}
           |  symbol: ${symbol.symbol}
           |  kind: ${symbol.kind}
           |  display_name: ${symbol.displayName}
           |  signature: ${symbol.signature}
           |)""".stripMargin)
      symbol.signature match {
        case Signature.Empty => ???
        case value: ValueSignature => ???
        case clazz: ClassSignature => processClass(clazz)
        case method: MethodSignature => ???
        case typ: TypeSignature => ???
      }
    }
  }

  private def processClass(clazz: ClassSignature): List[ClassDiagramElement] = {
    ???
  }
}
