package nz.co.bottech.scala2plantuml

import java.text.Normalizer
import scala.annotation.tailrec

private[scala2plantuml] object NaturalTypeOrdering extends Ordering[String] {

  implicit private class StringOps(val str: String) extends AnyVal {

    def compressWhitespace: String =
      str.strip().replaceAll("""\p{javaWhitespace}+""", " ")

    def normalize: String =
      Normalizer.normalize(str, Normalizer.Form.NFKC)

    def splitOnDigitsAndTypeSeparators: Array[String] =
      str.split("""(?=\p{Digit})(?<!\p{Digit})|(?<=\p{Digit})(?!\p{Digit})|(?=[#.])|(?<=[#.])""")
  }

  private def prepare(str: String): Array[String] =
    str.compressWhitespace.normalize.splitOnDigitsAndTypeSeparators

  private def isDigit(s: String) =
    Character.isDigit(s.charAt(0))

  private def diff(x: String, y: String): Int =
    if (isDigit(x) && isDigit(y)) math.signum(y.toLong - x.toLong).toInt
    else x.toLowerCase.compare(y.toLowerCase)

  override def compare(x: String, y: String): Int = {
    val xParts = prepare(x)
    val yParts = prepare(y)
    val end    = math.min(xParts.length, yParts.length)

    @tailrec
    def findFirstDifference(i: Int): Option[Int] =
      if (i < end)
        if (xParts(i) != yParts(i)) Some(i)
        else findFirstDifference(i + 1)
      else None

    findFirstDifference(0) match {
      case Some(i) => diff(xParts(i), yParts(i))
      case None    => xParts.length - yParts.length
    }
  }
}
