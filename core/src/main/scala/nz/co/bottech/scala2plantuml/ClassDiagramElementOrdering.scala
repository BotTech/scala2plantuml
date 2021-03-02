package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramElement.{Aggregation, Definition, Member, Type}

private[scala2plantuml] class ClassDiagramElementOrdering(symbolOrdering: Ordering[String])
    extends Ordering[ClassDiagramElement] {

  override def compare(x: ClassDiagramElement, y: ClassDiagramElement): Int =
    (x, y) match {
      case (xMember: Member, yMember: Member)                 => compareMembers(xMember, yMember)
      case (xMember: Member, yDefinition: Definition)         => compareToMember(xMember, yDefinition)
      case (xDefinition: Definition, yMember: Member)         => -compareToMember(yMember, xDefinition)
      case (xDefinition: Definition, yDefinition: Definition) => compareDefinitions(xDefinition, yDefinition)
      case (xAggregation: Aggregation, _)                     => compareToAggregation(xAggregation, y)
      case (_, yAggregation: Aggregation)                     => -compareToAggregation(yAggregation, x)
    }

  private def compareMembers(x: Member, y: Member): Int = {
    val xOwnerSymbol = x.ownerSymbol
    val yOwnerSymbol = y.ownerSymbol
    // If x and y are siblings then compare by symbols.
    if (xOwnerSymbol == yOwnerSymbol) compareDefinitions(x, y)
    // If y is a member of an inner type of x's owner then x comes first.
    else if (yOwnerSymbol.startsWith(xOwnerSymbol)) -1
    // If x is a member of an inner type of y's owner then x comes last.
    else if (xOwnerSymbol.startsWith(yOwnerSymbol)) 1
    // Otherwise they are unrelated so compare by symbols.
    else compareDefinitions(x, y)
  }

  private def compareToMember(x: Member, y: Definition): Int = {
    val xOwnerSymbol = x.ownerSymbol
    // If y is the owner of x then x comes last
    if (xOwnerSymbol == y.symbol) 1
    // If y is an inner type of x's owner then x comes first.
    else if (y.symbol.startsWith(xOwnerSymbol)) -1
    // Otherwise they are unrelated so compare by symbols.
    else compareDefinitions(x, y)
  }

  private def compareDefinitions(x: Definition, y: Definition): Int =
    symbolOrdering.compare(x.symbol, y.symbol)

  private def compareToAggregation(x: Aggregation, y: ClassDiagramElement): Int =
    y match {
      case yMember: Member =>
        val aggregatorOrder = symbolOrdering.compare(x.aggregator, yMember.ownerSymbol)
        if (aggregatorOrder == 0) 1
        else aggregatorOrder
      case yType: Type =>
        val aggregatorOrder = symbolOrdering.compare(x.aggregator, yType.symbol)
        if (aggregatorOrder == 0) 1
        else aggregatorOrder
      case yAggregation: Aggregation =>
        val aggregatorOrder = symbolOrdering.compare(x.aggregator, yAggregation.aggregator)
        if (aggregatorOrder == 0) symbolOrdering.compare(x.aggregated, yAggregation.aggregated)
        else aggregatorOrder
    }
}
