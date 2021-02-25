package nz.co.bottech.scala2plantuml.examples.method

import scala.annotation.nowarn

class PrivateThisMethod {

  @nowarn
  private[this] def method: String = "hello"
}
