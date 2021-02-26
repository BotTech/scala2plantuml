package nz.co.bottech.scala2plantuml

import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.meta.internal.semanticdb.SymbolOccurrence

private[scala2plantuml] class DefinitionIndex(loader: SemanticdbLoader) {

  private val logger = LoggerFactory.getLogger(classOf[DefinitionIndex])
  private val cache  = TrieMap.empty[String, Option[SymbolOccurrence]]

  def occurrence(symbol: String): Option[SymbolOccurrence] =
    cache.get(symbol) match {
      case Some(maybeOccurrence) => maybeOccurrence
      case None =>
        loadSymbol(symbol)
        cache.getOrElseUpdate(symbol, None)
    }

  private def loadSymbol(symbol: String): Unit =
    loader.load(symbol) match {
      case Left(errors) =>
        logger.warn(s"Could not find symbol: $symbol")
        errors.foreach(error => logger.warn(s"- $error"))
      case Right(textDocuments) =>
        val definitions = textDocuments.flatMap(_.occurrences.filter(_.role == SymbolOccurrence.Role.DEFINITION))
        cache ++= definitions.map(occurrence => occurrence.symbol -> Some(occurrence))
    }
}
