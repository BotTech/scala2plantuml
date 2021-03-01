package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramElement.{Aggregation, Definition, Member, Type}

private[scala2plantuml] class ClassDiagramElementOrdering(symbolOrdering: Ordering[String])
    extends Ordering[ClassDiagramElement] {

  override def compare(x: ClassDiagramElement, y: ClassDiagramElement): Int =
    x match {
      case xType: Type               => compareToType(xType, y)
      case xMember: Member           => compareToMember(xMember, y)
      case xAggregation: Aggregation => compareToAggregation(xAggregation, y)
    }

  private def compareToType(x: Type, y: ClassDiagramElement): Int =
    y match {
      case yMember: Member if x.owns(yMember) => -1
      case yMember: Member                    => -compareToMember(yMember, x)
      case yDefinition: Definition            => -compareToDefinition(yDefinition, x)
      case yAggregation: Aggregation          => -compareToAggregation(yAggregation, x)
    }

  private def compareToMember(x: Member, y: ClassDiagramElement): Int =
    y match {
      case yType: Type if yType.owns(x)                                            => 1
      case yMember: Member if x.ownerSymbol == yMember.ownerSymbol                 => symbolOrdering.compare(x.symbol, yMember.symbol)
      case yDefinition: Definition if yDefinition.symbol.startsWith(x.ownerSymbol) => -1
      case yDefinition: Definition if x.symbol.startsWith(yDefinition.ownerSymbol) => 1
      case yDefinition: Definition                                                 => -compareToDefinition(yDefinition, x)
      case yAggregation: Aggregation                                               => -compareToAggregation(yAggregation, x)
    }

  private def compareToDefinition(x: Definition, y: ClassDiagramElement): Int =
    y match {
      case yDefinition: Definition   => symbolOrdering.compare(x.symbol, yDefinition.symbol)
      case yAggregation: Aggregation => -compareToAggregation(yAggregation, x)
    }

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
