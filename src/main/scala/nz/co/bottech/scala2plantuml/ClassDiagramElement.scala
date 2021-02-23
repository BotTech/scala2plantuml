package nz.co.bottech.scala2plantuml

sealed trait ClassDiagramElement {
  def displayName: String
  def fullName: String

  def isObject: Boolean
}

final case class UmlAbstractClass(displayName: String, fullName: String) extends ClassDiagramElement {
  override def isObject: Boolean = false
}

final case class UmlAnnotation(displayName: String, fullName: String, isObject: Boolean) extends ClassDiagramElement

final case class UmlEnum(displayName: String, fullName: String, isObject: Boolean) extends ClassDiagramElement

final case class UmlClass(displayName: String, fullName: String, isObject: Boolean) extends ClassDiagramElement

final case class UmlInterface(displayName: String, fullName: String) extends ClassDiagramElement {
  override def isObject: Boolean = false
}
