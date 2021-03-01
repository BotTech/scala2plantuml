package nz.co.bottech.scala2plantuml.examples.methodtypeparameters

trait TypeParameterWithMultipleBounds {

  def foo[A <: Trait with Trait2]: String
}
