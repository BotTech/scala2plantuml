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
        val symbolTable     = aggregateSymbolTable(textDocuments, loader)
        val typeIndex       = new TypeIndex(symbolTable)
        val definitionIndex = new DefinitionIndex(loader)
        textDocuments.flatMap(SemanticProcessor.processDocument(_, typeIndex, definitionIndex))
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
