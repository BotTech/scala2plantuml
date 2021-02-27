package nz.co.bottech.scala2plantuml

import scala.meta.internal.semanticdb.Scala._

sealed trait ClassDiagramElement {
  def displayName: String
  def symbol: String

  def isObject: Boolean
  def isType: Boolean
  def isMember: Boolean
  def isAbstract: Boolean

  def rename(displayName: String): ClassDiagramElement

  def isParentOf(child: ClassDiagramElement): Boolean = child.parentSymbol == symbol
  def parentSymbol: String                            = symbol.ownerChain.takeRight(2).headOption.getOrElse(symbol)
}

object ClassDiagramElement {

  final case class Annotation(displayName: String, symbol: String, isObject: Boolean) extends ClassDiagramElement {
    override val isType: Boolean     = true
    override val isMember: Boolean   = false
    override val isAbstract: Boolean = false

    override def rename(displayName: String): ClassDiagramElement = copy(displayName = displayName)
  }

  final case class Class(displayName: String, symbol: String, isObject: Boolean, isAbstract: Boolean)
      extends ClassDiagramElement {
    override val isType: Boolean   = true
    override val isMember: Boolean = false

    override def rename(displayName: String): ClassDiagramElement = copy(displayName = displayName)
  }

  final case class Enum(displayName: String, symbol: String, isObject: Boolean) extends ClassDiagramElement {
    override val isType: Boolean     = true
    override val isMember: Boolean   = false
    override val isAbstract: Boolean = false

    override def rename(displayName: String): ClassDiagramElement = copy(displayName = displayName)
  }

  sealed trait Visibility

  object Visibility {
    case object Private        extends Visibility
    case object Protected      extends Visibility
    case object PackagePrivate extends Visibility
    case object Public         extends Visibility
  }

  final case class Field(displayName: String, symbol: String, visibility: Visibility, isAbstract: Boolean)
      extends ClassDiagramElement {
    override val isObject: Boolean = false
    override val isType: Boolean   = false
    override val isMember: Boolean = true

    override def rename(displayName: String): ClassDiagramElement = copy(displayName = displayName)
  }

  final case class Interface(displayName: String, symbol: String) extends ClassDiagramElement {
    override val isObject: Boolean   = false
    override val isType: Boolean     = true
    override val isMember: Boolean   = false
    override val isAbstract: Boolean = true

    override def rename(displayName: String): ClassDiagramElement = copy(displayName = displayName)
  }

  final case class Method(
      displayName: String,
      symbol: String,
      visibility: Visibility,
      constructor: Boolean,
      synthetic: Boolean,
      isAbstract: Boolean)
      extends ClassDiagramElement {
    override val isObject: Boolean = false
    override val isType: Boolean   = false
    override val isMember: Boolean = true

    override def rename(displayName: String): ClassDiagramElement = copy(displayName = displayName)
  }
}
