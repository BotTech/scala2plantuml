package nz.co.bottech.scala2plantuml.examples.method

class HigherKindedParameter {

  def method[A[_], B](a: A[B]): String = a.toString
}
