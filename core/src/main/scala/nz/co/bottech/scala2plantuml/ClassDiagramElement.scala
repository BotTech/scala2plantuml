package nz.co.bottech.scala2plantuml

sealed trait ClassDiagramElement

object ClassDiagramElement {

  sealed trait Definition extends ClassDiagramElement {
    def symbol: String
    def displayName: String

    def ownerSymbol: String = symbolOwner(symbol)
  }

  sealed trait Parameterised extends Definition {
    def typeParameters: Seq[TypeParameter]
  }

  final case class TypeParameter(symbol: String, parentSymbols: Seq[String])

  sealed trait Type extends Parameterised {
    def isObject: Boolean
    def parentSymbols: Seq[String]

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

  sealed trait Member extends Definition {
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
      with Parameterised

  final case class Aggregation(aggregator: String, aggregated: String) extends ClassDiagramElement
}
