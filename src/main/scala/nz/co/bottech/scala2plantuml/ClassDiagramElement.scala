package nz.co.bottech.scala2plantuml

sealed trait ClassDiagramElement

final case class AbstractClass(displayName: String) extends ClassDiagramElement

final case class Annotation(displayName: String) extends ClassDiagramElement

final case class ConcreteClass(displayName: String) extends ClassDiagramElement
