package nz.co.bottech.scala2plantuml

import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent.TrieMap
import scala.jdk.CollectionConverters._
import scala.meta.internal.semanticdb.SymbolOccurrence

class DefinitionIndex(loader: SemanticDbLoader) {

  private val cache  = TrieMap.empty[String, Option[SymbolOccurrence]]
  private val loaded = ConcurrentHashMap.newKeySet[String]().asScala

  def occurrence(symbol: String): Option[SymbolOccurrence] =
    cache.get(symbol) match {
      case Some(maybeOccurrence) => maybeOccurrence
      case None =>
        loadSymbol(symbol)
        cache.getOrElseUpdate(symbol, None)
    }

  private def loadSymbol(symbol: String): Unit =
    if (!loaded.contains(symbol))
      loaded.synchronized {
        if (!loaded.contains(symbol)) {
          loader.load(symbol).foreach { textDocuments =>
            val definitions = textDocuments.flatMap(_.occurrences.filter(_.role == SymbolOccurrence.Role.DEFINITION))
            cache.addAll(definitions.map(occurrence => occurrence.symbol -> Some(occurrence)))
          }
          val _ = loaded.add(symbol)
        }
      }
}
