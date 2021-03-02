package nz.co.bottech.scala2plantuml.examples.method

class HigherKindedParameterFilled {

  def method[A[_]](a: A[Trait]): String = a.toString
}
