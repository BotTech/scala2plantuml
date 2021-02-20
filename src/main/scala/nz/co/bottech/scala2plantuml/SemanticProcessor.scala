package nz.co.bottech.scala2plantuml

import org.slf4j.LoggerFactory

import java.io.InputStream
import scala.meta.internal.semanticdb.{ClassSignature, MethodSignature, Signature, TextDocument, TextDocuments, TypeSignature, ValueSignature}
import scala.util.Try

object ClassDiagramGenerator {

  private val logger = LoggerFactory.getLogger(classOf[ClassDiagramGenerator.type])

  def fromInputStream(inputStream: InputStream): Try[String] = {
    Try {
      val documents = TextDocuments.parseFrom(inputStream).documents
      val elements = documents.flatMap(processDocument)
      ClassDiagramPrinter.print(elements)
    }
  }

  private def processDocument(document: TextDocument): Seq[ClassDiagramElement] = {
    document.symbols.flatMap { symbol =>
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
