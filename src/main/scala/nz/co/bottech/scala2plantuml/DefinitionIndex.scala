package nz.co.bottech.scala2plantuml

import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.meta.internal.semanticdb.SymbolOccurrence

private[scala2plantuml] class DefinitionIndex(loader: SemanticDbLoader) {

  private val logger = LoggerFactory.getLogger(classOf[DefinitionIndex])
  private val cache  = TrieMap.empty[String, Option[SymbolOccurrence]]

  def occurrence(symbol: String): Option[SymbolOccurrence] =
    if (scalaStdLibSymbol(symbol)) None
    else
      cache.get(symbol) match {
        case Some(maybeOccurrence) => maybeOccurrence
        case None =>
          loadSymbol(symbol)
          cache.getOrElseUpdate(symbol, None)
      }

  private def loadSymbol(symbol: String): Unit =
    loader.load(symbol) match {
      case Left(error) =>
        logger.warn(error)
      case Right(textDocuments) =>
        val definitions = textDocuments.flatMap(_.occurrences.filter(_.role == SymbolOccurrence.Role.DEFINITION))
        cache.addAll(definitions.map(occurrence => occurrence.symbol -> Some(occurrence)))
    }
}
