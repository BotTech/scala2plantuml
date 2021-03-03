package nz.co.bottech.scala2plantuml.examples.method

class ExistentialParameter {

  def method[A[_]](a: A[_]): String = a.toString
}
