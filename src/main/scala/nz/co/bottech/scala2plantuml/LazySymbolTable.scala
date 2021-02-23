package nz.co.bottech.scala2plantuml

import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent.TrieMap
import scala.jdk.CollectionConverters._
import scala.meta.internal.semanticdb.SymbolInformation
import scala.meta.internal.symtab.SymbolTable

class LazySymbolTable(loader: SemanticDbLoader) extends SymbolTable {

  private val symbolCache = TrieMap.empty[String, SymbolInformation]
  private val loaded      = ConcurrentHashMap.newKeySet[String]().asScala

  override def info(symbol: String): Option[SymbolInformation] =
    symbolCache.get(symbol) match {
      case some: Some[_] => some
      case None =>
        loadSymbol(symbol)
        symbolCache.get(symbol)
    }

  private def loadSymbol(symbol: String): Unit =
    if (!loaded.contains(symbol))
      loaded.synchronized {
        if (!loaded.contains(symbol)) {
          loader.load(symbol).foreach { textDocuments =>
            val symbols = textDocuments.flatMap(_.symbols).map(info => info.symbol -> info)
            symbolCache.addAll(symbols)
          }
          loaded.add(symbol)
        }
      }
}
