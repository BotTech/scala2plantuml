package nz.co.bottech.scala2plantuml.examples.method

import scala.annotation.nowarn

class PrivateMethod {

  @nowarn
  private def method: String = "hello"
}
