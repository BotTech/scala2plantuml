package nz.co.bottech.scala2plantuml

import scala.collection.mutable
import scala.meta.{Defn, Mod, Traverser, Tree}

class SourceTraverser extends Traverser {

  private val elements: mutable.ListBuffer[ClassDiagramElement] = mutable.ListBuffer.empty

  def result(): List[ClassDiagramElement] = elements.toList

  override def apply(tree: Tree): Unit = {
    tree match {
      case clazz: Defn.Class => traverseClass(clazz)
      case _ => super.apply(tree)
    }
  }

  private def traverseClass(clazz: Defn.Class): Unit = {
    val element = if (clazz.mods.exists(_.isInstanceOf[Mod.Abstract])) {
      AbstractClass(clazz.name.value)
    } else {
      ConcreteClass(clazz.name.value)
    }
    elements.addOne(element)
    super.apply(clazz)
  }
}
