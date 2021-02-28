package nz.co.bottech.scala2plantuml

import scala.meta.internal.semanticdb.Scala._

sealed trait ClassDiagramElement

object ClassDiagramElement {

  trait Definition {
    def symbol: String
    def displayName: String

    def ownerSymbol: String = symbol.ownerChain.takeRight(2).headOption.getOrElse(symbol)
  }

  final case class TypeParameter(symbol: String, parentSymbols: Seq[String])

  sealed trait Type extends ClassDiagramElement with Definition {
    def isObject: Boolean
    def parentSymbols: Seq[String]
    def typeParameters: Seq[TypeParameter]

    def owns(child: Definition): Boolean = child.ownerSymbol == symbol
  }

  final case class Annotation(
      displayName: String,
      symbol: String,
      isObject: Boolean,
      parentSymbols: Seq[String],
      typeParameters: Seq[TypeParameter])
      extends Type

  final case class Class(
      displayName: String,
      symbol: String,
      isObject: Boolean,
      isAbstract: Boolean,
      parentSymbols: Seq[String],
      typeParameters: Seq[TypeParameter])
      extends Type

  final case class Enum(
      displayName: String,
      symbol: String,
      isObject: Boolean,
      parentSymbols: Seq[String],
      typeParameters: Seq[TypeParameter])
      extends Type

  final case class Interface(
      displayName: String,
      symbol: String,
      parentSymbols: Seq[String],
      typeParameters: Seq[TypeParameter])
      extends Type {
    override val isObject: Boolean = false
  }

  sealed trait Visibility

  object Visibility {
    case object Private        extends Visibility
    case object Protected      extends Visibility
    case object PackagePrivate extends Visibility
    case object Public         extends Visibility
  }

  sealed trait Member extends ClassDiagramElement with Definition {
    def visibility: Visibility
    def isAbstract: Boolean
  }

  final case class Field(displayName: String, symbol: String, visibility: Visibility, isAbstract: Boolean)
      extends Member

  final case class Method(
      displayName: String,
      symbol: String,
      visibility: Visibility,
      constructor: Boolean,
      synthetic: Boolean,
      isAbstract: Boolean,
      typeParameters: Seq[TypeParameter])
      extends Member

  final case class Aggregation(aggregator: String, aggregated: String) extends ClassDiagramElement
}
