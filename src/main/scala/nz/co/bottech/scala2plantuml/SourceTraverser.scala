package nz.co.bottech.scala2plantuml

import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.meta.{Defn, Mod, Traverser, Tree}

class SourceTraverser extends Traverser {

  private val logger = LoggerFactory.getLogger(classOf[SourceTraverser])

  private val classes: mutable.ListBuffer[ClassSyntax] = mutable.ListBuffer.empty

  def result(): SourceSyntax =
    SourceSyntax(
      classes = classes.toList
    )

  override def apply(tree: Tree): Unit = {
    logger.trace(tree.toString)
    logger.trace(debug(tree))
    tree match {
      case clazz: Defn.Class => traverseClass(clazz)
      case _                 => super.apply(tree)
    }
  }

  private def traverseClass(clazz: Defn.Class): Unit = {
    val name = clazz.name.value
    classes.addOne(ClassSyntax(name, isAbstract(clazz)))
    super.apply(clazz)
  }

  private def isAbstract(clazz: Defn.Class): Boolean =
    clazz.mods.exists(_.isInstanceOf[Mod.Abstract])

  private def debug(tree: Tree): String =
    s"${tree.productPrefix}: ${tree.toString}"
}
