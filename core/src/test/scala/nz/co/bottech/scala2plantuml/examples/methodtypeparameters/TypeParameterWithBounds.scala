package nz.co.bottech.scala2plantuml.examples.methodtypeparameters

trait TypeParameterWithBounds {

  def foo[A <: Trait]: String
}
