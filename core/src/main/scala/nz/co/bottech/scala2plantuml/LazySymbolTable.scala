package nz.co.bottech.scala2plantuml

import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.meta.internal.semanticdb.SymbolInformation
import scala.meta.internal.symtab.SymbolTable

private[scala2plantuml] class LazySymbolTable(loader: SemanticDbLoader) extends SymbolTable {

  private val logger = LoggerFactory.getLogger(classOf[LazySymbolTable])
  private val cache  = TrieMap.empty[String, SymbolInformation]

  override def info(symbol: String): Option[SymbolInformation] =
    if (scalaStdLibSymbol(symbol)) None
    else
      cache.get(symbol) match {
        case some: Some[_] => some
        case None =>
          loadSymbol(symbol)
          cache.get(symbol)
      }

  private def loadSymbol(symbol: String): Unit =
    loader.load(symbol) match {
      case Left(error) =>
        logger.warn(error)
      case Right(textDocuments) =>
        val symbols = textDocuments.flatMap(_.symbols).map(info => info.symbol -> info)
        cache.addAll(symbols)
    }
}
