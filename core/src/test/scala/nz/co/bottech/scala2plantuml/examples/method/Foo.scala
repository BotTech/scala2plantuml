package nz.co.bottech.scala2plantuml.examples.method

trait Foo

trait Bar {

  def bar[A[_]](foo: A[Foo]): Unit
}
