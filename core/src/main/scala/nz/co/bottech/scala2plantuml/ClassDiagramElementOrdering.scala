package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramElement.{Aggregation, Definition}

private[scala2plantuml] class ClassDiagramElementOrdering(symbolOrdering: Ordering[String])
    extends Ordering[ClassDiagramElement] {

  override def compare(x: ClassDiagramElement, y: ClassDiagramElement): Int =
    // TODO: Move the logic from NaturalTypeOrdering to here.
    x match {
      case xDefinition: Definition =>
        y match {
          case yDefinition: Definition => symbolOrdering.compare(xDefinition.symbol, yDefinition.symbol)
          case yAggregation: Aggregation =>
            val aggregatorOrder = symbolOrdering.compare(xDefinition.symbol, yAggregation.aggregator)
            if (aggregatorOrder == 0) -1
            else aggregatorOrder
        }
      case xAggregation: Aggregation =>
        y match {
          case yDefinition: Definition =>
            val aggregatorOrder = symbolOrdering.compare(xAggregation.aggregator, yDefinition.symbol)
            if (aggregatorOrder == 0) 1
            else aggregatorOrder
          case yAggregation: Aggregation =>
            val aggregatorOrder = symbolOrdering.compare(xAggregation.aggregator, yAggregation.aggregator)
            if (aggregatorOrder == 0) symbolOrdering.compare(xAggregation.aggregated, yAggregation.aggregated)
            else aggregatorOrder
        }
    }
}
