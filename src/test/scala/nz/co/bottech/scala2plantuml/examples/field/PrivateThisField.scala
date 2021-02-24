package nz.co.bottech.scala2plantuml.examples.field

import scala.annotation.nowarn

class PrivateThisField {

  @nowarn
  private[this] val field: String = "hello"
}
