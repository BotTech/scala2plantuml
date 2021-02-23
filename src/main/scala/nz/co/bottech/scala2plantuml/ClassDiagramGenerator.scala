package nz.co.bottech.scala2plantuml

import scala.meta.internal.semanticdb.TextDocument
import scala.meta.internal.symtab.{AggregateSymbolTable, GlobalSymbolTable, LocalSymbolTable}
import scala.meta.io.Classpath

object ClassDiagramGenerator {

  def basedOn(
      symbol: String,
      prefixes: Seq[String],
      classloader: ClassLoader
    ): Either[String, Seq[ClassDiagramElement]] = {
    val loader = new SemanticDbLoader(prefixes, classloader)
    loader
      .load(symbol)
      .map { textDocuments =>
        val symbolTable = aggregateSymbolTable(textDocuments, loader)
        val index       = new TypeIndex(symbolTable)
        textDocuments.flatMap(SemanticProcessor.processDocument(_, index))
      }
  }

  private def aggregateSymbolTable(textDocuments: Seq[TextDocument], loader: SemanticDbLoader) =
    AggregateSymbolTable(
      List(
        LocalSymbolTable(textDocuments.flatMap(_.symbols)),
        new LazySymbolTable(loader),
        GlobalSymbolTable(Classpath(Nil))
      )
    )

}
