package nz.co.bottech.scala2plantuml

import scala.meta.internal.symtab.{AggregateSymbolTable, GlobalSymbolTable}
import scala.meta.io.Classpath

object ClassDiagramGenerator {

  def fromSymbol(
      symbol: String,
      prefixes: Seq[String],
      ignore: String => Boolean,
      classloader: ClassLoader
    ): Seq[ClassDiagramElement] = {
    val loader          = new SemanticDbLoader(prefixes, classloader)
    val symbolTable     = aggregateSymbolTable(loader)
    val typeIndex       = new TypeIndex(symbolTable)
    val definitionIndex = new DefinitionIndex(loader)
    SemanticProcessor.processSymbol(symbol, ignore, symbolTable, typeIndex, definitionIndex)
  }

  private def aggregateSymbolTable(loader: SemanticDbLoader) =
    AggregateSymbolTable(
      List(
        new LazySymbolTable(loader),
        GlobalSymbolTable(Classpath(Nil))
      )
    )

}
