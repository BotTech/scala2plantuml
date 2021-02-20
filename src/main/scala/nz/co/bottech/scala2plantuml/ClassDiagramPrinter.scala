package nz.co.bottech.scala2plantuml

import scala.annotation.tailrec

object ClassDiagramPrinter {

  private val SomewhatSensibleName = """[\p{Alnum}_-]+""".r

  def print(elements: List[ClassDiagramElement]): String = {
    @tailrec
    def loop(remaining: List[ClassDiagramElement], acc: StringBuilder): String = {
      remaining match {
        case Nil => acc.toString
        case head :: tail =>
          head match {
            case AbstractClass(name) =>
              acc.append("abstract class ")
              acc.append(quoteName(name))
            case ConcreteClass(name) =>
              acc.append("class ")
              acc.append(quoteName(name))
          }
          loop(tail, acc)
      }
    }
    loop(elements, new StringBuilder)
  }

  private def quoteName(name: String): String = {
    if (SomewhatSensibleName.matches(name)) name
    else s""""$name""""
  }
}
