package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.NaturalOrdering.{PartsOrdering, StringOps}

import java.text.Normalizer
import scala.annotation.tailrec

private[scala2plantuml] class NaturalOrdering extends Ordering[String] {

  override def compare(x: String, y: String): Int = {
    // Ensure that nested types come after all other members.
    val xLevel = level(x)
    val yLevel = level(y)
    if (xLevel != yLevel) xLevel - yLevel
    else PartsOrdering.compare(prepare(x), prepare(y))
  }

  private def level(s: String): Int = s.count(_ == '#')

  private def prepare(str: String): Array[String] =
    str.compressWhitespace.normalize.separate

}

private[scala2plantuml] object NaturalOrdering {

  private object PartsOrdering extends Ordering[Array[String]] {

    override def compare(x: Array[String], y: Array[String]): Int = {
      val end = math.min(x.length, y.length)

      @tailrec
      def findFirstDifference(i: Int): Option[Int] =
        if (i < end)
          if (x(i) != y(i)) Some(i)
          else findFirstDifference(i + 1)
        else None

      def isDigit(s: String) =
        Character.isDigit(s.charAt(0))

      def diff(x: String, y: String): Int =
        if (isDigit(x) && isDigit(y))
          math.signum(y.toLong - x.toLong).toInt
        else
          x.compare(y)

      findFirstDifference(0) match {
        case Some(i) => diff(x(i), y(i))
        case None    => y.length - x.length
      }
    }
  }

  implicit class StringOps(val str: String) extends AnyVal {

    def compressWhitespace: String =
      str.strip().replaceAll("""\p{javaWhitespace}+""", " ")

    def normalize: String =
      Normalizer.normalize(str, Normalizer.Form.NFKC)

    def separate: Array[String] =
      str.split("""(?=\p{Digit})(?<!\p{Digit})|(?<=\p{Digit})(?!\p{Digit})""")
  }
}
