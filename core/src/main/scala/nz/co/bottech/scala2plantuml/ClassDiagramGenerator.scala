package nz.co.bottech.scala2plantuml

import scala.meta.internal.symtab.{AggregateSymbolTable, GlobalSymbolTable}
import scala.meta.io.Classpath

object ClassDiagramGenerator {

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def fromSymbol(
      symbol: String,
      prefixes: Seq[String],
      ignore: String => Boolean,
      classloader: ClassLoader,
      maxLevel: Option[Int] = None
    ): Seq[ClassDiagramElement] = {
    val loader          = new SemanticDBLoader(prefixes, classloader)
    val symbolTable     = aggregateSymbolTable(loader)
    val symbolIndex     = new SymbolIndex(ignore, symbolTable)
    val typeIndex       = new TypeIndex(symbolIndex)
    val definitionIndex = new DefinitionIndex(loader)
    SymbolProcessor.processSymbol(symbol, maxLevel, symbolIndex, typeIndex, definitionIndex)
  }

  private def aggregateSymbolTable(loader: SemanticDBLoader) =
    AggregateSymbolTable(
      List(
        new LazySymbolTable(loader),
        GlobalSymbolTable(Classpath(Nil))
      )
    )

}
