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
    val loader          = new SemanticdbLoader(prefixes, classloader)
    val symbolTable     = aggregateSymbolTable(loader)
    val symbolIndex     = new SymbolIndex(ignore, symbolTable)
    val typeIndex       = new TypeIndex(symbolIndex)
    val definitionIndex = new DefinitionIndex(loader)
    SymbolProcessor.processSymbol(symbol, symbolIndex, typeIndex, definitionIndex)
  }

  private def aggregateSymbolTable(loader: SemanticdbLoader) =
    AggregateSymbolTable(
      List(
        new LazySymbolTable(loader),
        GlobalSymbolTable(Classpath(Nil))
      )
    )

}
