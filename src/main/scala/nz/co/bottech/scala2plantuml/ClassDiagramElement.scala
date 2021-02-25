package nz.co.bottech.scala2plantuml

sealed trait ClassDiagramElement {
  def displayName: String
  def symbol: String
}

object ClassDiagramElement {

  def isObject(element: ClassDiagramElement): Boolean =
    element match {
      case _: AbstractClass       => false
      case annotation: Annotation => annotation.isObject
      case clazz: Class           => clazz.isObject
      case enm: Enum              => enm.isObject
      case _: Field               => false
      case _: Interface           => false
      case _: Method              => false
    }

  final case class AbstractClass(displayName: String, symbol: String) extends ClassDiagramElement

  final case class Annotation(displayName: String, symbol: String, isObject: Boolean) extends ClassDiagramElement

  final case class Class(displayName: String, symbol: String, isObject: Boolean) extends ClassDiagramElement

  final case class Enum(displayName: String, symbol: String, isObject: Boolean) extends ClassDiagramElement

  sealed trait Visibility

  object Visibility {
    case object Private        extends Visibility
    case object Protected      extends Visibility
    case object PackagePrivate extends Visibility
    case object Public         extends Visibility
  }

  final case class Field(displayName: String, symbol: String, visibility: Visibility) extends ClassDiagramElement

  final case class Interface(displayName: String, symbol: String) extends ClassDiagramElement

  final case class Method(
      displayName: String,
      symbol: String,
      visibility: Visibility,
      constructor: Boolean,
      synthetic: Boolean)
      extends ClassDiagramElement
}
