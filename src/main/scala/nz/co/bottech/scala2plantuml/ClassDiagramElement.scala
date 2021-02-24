package nz.co.bottech.scala2plantuml

sealed trait ClassDiagramElement {
  def displayName: String
  def symbol: String

  def isObject: Boolean
}

// TODO: This is weird. Get rid of it.
trait NonObject {
  self: ClassDiagramElement =>
  final override val isObject: Boolean = false
}

final case class UmlAbstractClass(displayName: String, symbol: String) extends ClassDiagramElement with NonObject

final case class UmlAnnotation(displayName: String, symbol: String, isObject: Boolean) extends ClassDiagramElement

final case class UmlClass(displayName: String, symbol: String, isObject: Boolean) extends ClassDiagramElement

final case class UmlEnum(displayName: String, symbol: String, isObject: Boolean) extends ClassDiagramElement

sealed trait UmlVisibility

object UmlVisibility {
  case object Private        extends UmlVisibility
  case object Protected      extends UmlVisibility
  case object PackagePrivate extends UmlVisibility
  case object Public         extends UmlVisibility
}

final case class UmlField(displayName: String, symbol: String, visibility: UmlVisibility)
    extends ClassDiagramElement
    with NonObject

final case class UmlInterface(displayName: String, symbol: String) extends ClassDiagramElement with NonObject

final case class UmlMethod(
    displayName: String,
    symbol: String,
    visibility: UmlVisibility,
    constructor: Boolean,
    synthetic: Boolean)
    extends ClassDiagramElement
    with NonObject
