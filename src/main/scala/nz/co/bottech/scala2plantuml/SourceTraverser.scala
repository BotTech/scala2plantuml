package nz.co.bottech.scala2plantuml

import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.meta.{Defn, Mod, Template, Traverser, Tree}
import scala.meta.quasiquotes._
import scala.meta.contrib._

class SourceTraverser extends Traverser {

  private val logger                                            = LoggerFactory.getLogger(classOf[SourceTraverser])
  private val elements: mutable.ListBuffer[ClassDiagramElement] = mutable.ListBuffer.empty

  def result(): List[ClassDiagramElement] = elements.toList

  override def apply(tree: Tree): Unit = {
    logger.trace(debug(tree))
    tree match {
      case clazz: Defn.Class => traverseClass(clazz)
      case _                 => super.apply(tree)
    }
  }

  private def traverseClass(clazz: Defn.Class): Unit = {
    val name = clazz.name.value
    val element =
      if (isAbstract(clazz))
        AbstractClass(name)
      else if (isAnnotation(clazz))
        Annotation(name)
      else
        ConcreteClass(name)
    elements.addOne(element)
    super.apply(clazz)
  }

  private def isAbstract(clazz: Defn.Class): Boolean =
    clazz.mods.exists(_.isInstanceOf[Mod.Abstract])

  private def isAnnotation(clazz: Defn.Class): Boolean = {
    clazz.templ.isEqual(template"scala.annotation.Annotation")
  }

  private def debug(tree: Tree): String =
    s"${tree.productPrefix}: ${tree.toString}"
}
