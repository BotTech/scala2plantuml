package nz.co.bottech.scala2plantuml

sealed trait ClassDiagramElement

final case class AbstractClass(name: String) extends ClassDiagramElement

final case class Annotation(name: String) extends ClassDiagramElement

final case class ConcreteClass(name: String) extends ClassDiagramElement
