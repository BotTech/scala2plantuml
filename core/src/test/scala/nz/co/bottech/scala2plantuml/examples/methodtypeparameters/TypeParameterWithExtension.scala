package nz.co.bottech.scala2plantuml.examples.methodtypeparameters

trait TypeParameterWithExtension[A <: Trait] {

  def foo[B <: Trait]: String
}
