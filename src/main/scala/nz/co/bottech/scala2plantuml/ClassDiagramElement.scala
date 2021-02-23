package nz.co.bottech.scala2plantuml

sealed trait ClassDiagramElement {
  def displayName: String
  def fullName: String

  def isObject: Boolean
}

final case class AbstractClass(displayName: String, fullName: String) extends ClassDiagramElement {
  override def isObject: Boolean = false
}

final case class Annotation(displayName: String, fullName: String, isObject: Boolean) extends ClassDiagramElement

final case class ConcreteClass(displayName: String, fullName: String, isObject: Boolean) extends ClassDiagramElement
