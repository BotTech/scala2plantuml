package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramPrinter.Options

import scala.meta.internal.semanticdb.TextDocument
import scala.meta.internal.symtab.{AggregateSymbolTable, GlobalSymbolTable, LocalSymbolTable}
import scala.meta.io.Classpath

object ClassDiagramGenerator {

  def basedOn(
      symbol: String,
      prefixes: Seq[String],
      classloader: ClassLoader
    ): Either[String, String] = {
    val loader = new SemanticDbLoader(prefixes, classloader)
    loader
      .load(symbol)
      .map { textDocuments =>
        val symbolTable = aggregateSymbolTable(textDocuments, loader)
        textDocuments.flatMap(SemanticProcessor.processDocument(_, symbolTable))
      }
      .map { elements =>
        ClassDiagramPrinter.print(elements, Options.default)
      }
  }

  private def aggregateSymbolTable(textDocuments: Seq[TextDocument], loader: SemanticDbLoader) =
    AggregateSymbolTable(
      List(
        LocalSymbolTable(textDocuments.flatMap(_.symbols)),
        new LazySymbolTable(loader),
        GlobalSymbolTable(Classpath(Nil)),
      )
    )

}
