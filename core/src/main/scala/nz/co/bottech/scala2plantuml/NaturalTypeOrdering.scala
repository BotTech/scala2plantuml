package nz.co.bottech.scala2plantuml

import java.text.Normalizer
import scala.annotation.tailrec

private[scala2plantuml] object NaturalTypeOrdering extends Ordering[String] {

  private object TypeOrdering extends Ordering[String] {

    override def compare(x: String, y: String): Int = {
      val xParts = x.splitOnTypes
      val yParts = y.splitOnTypes
      if (xParts.length < yParts.length)
        if (yParts.startsWith(xParts.init)) -1
        else 0
      else if (yParts.length < xParts.length)
        if (xParts.startsWith(yParts.init)) 1
        else 0
      else 0
    }
  }

  private object PartsOrdering extends Ordering[String] {

    override def compare(x: String, y: String): Int = {
      val xParts = x.splitOnDigits
      val yParts = y.splitOnDigits
      val end    = math.min(xParts.length, yParts.length)

      @tailrec
      def findFirstDifference(i: Int): Option[Int] =
        if (i < end)
          if (xParts(i) != yParts(i)) Some(i)
          else findFirstDifference(i + 1)
        else None

      def isDigit(s: String) =
        Character.isDigit(s.charAt(0))

      def diff(x: String, y: String): Int =
        if (isDigit(x) && isDigit(y)) math.signum(y.toLong - x.toLong).toInt
        else x.toLowerCase.compare(y.toLowerCase)

      findFirstDifference(0) match {
        case Some(i) => diff(xParts(i), yParts(i))
        case None    => xParts.length - yParts.length
      }
    }
  }

  implicit class StringOps(val str: String) extends AnyVal {

    def compressWhitespace: String =
      str.strip().replaceAll("""\p{javaWhitespace}+""", " ")

    def normalize: String =
      Normalizer.normalize(str, Normalizer.Form.NFKC)

    def splitOnTypes: Array[String] =
      str.split("""(?<=[#.])""", -1)

    def splitOnDigits: Array[String] =
      str.split("""(?=\p{Digit})(?<!\p{Digit})|(?<=\p{Digit})(?!\p{Digit})|(?=[#.])|(?<=[#.])""")
  }

  override def compare(x: String, y: String): Int = {
    val xPrepared = prepare(x)
    val yPrepared = prepare(y)
    val typeOrder = TypeOrdering.compare(xPrepared, yPrepared)
    if (typeOrder == 0) PartsOrdering.compare(xPrepared, yPrepared)
    else typeOrder
  }

  private def prepare(str: String): String =
    str.compressWhitespace.normalize
}
