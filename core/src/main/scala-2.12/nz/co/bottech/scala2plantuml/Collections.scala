package nz.co.bottech.scala2plantuml

import scala.collection.{compat, TraversableOnce}

object Collections {

  type IterableOnce[+A] = compat.IterableOnce[A]
  val IterableOnce: TraversableOnce.type = compat.IterableOnce

}
