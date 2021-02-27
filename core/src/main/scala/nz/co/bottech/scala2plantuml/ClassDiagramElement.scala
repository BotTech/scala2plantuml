package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.ClassDiagramElement.{Member, Type}

import scala.meta.internal.semanticdb.Scala._

sealed trait ClassDiagramElement {
  def displayName: String
  def symbol: String

  def isObject: Boolean
  def isAbstract: Boolean

  def isType: Boolean =
    this match {
      case _: Type   => true
      case _: Member => false
    }

  def isMember: Boolean =
    this match {
      case _: Type   => false
      case _: Member => true
    }

  def rename(displayName: String): ClassDiagramElement

  def owns(child: ClassDiagramElement): Boolean = child.ownerSymbol == symbol
  def ownerSymbol: String                       = symbol.ownerChain.takeRight(2).headOption.getOrElse(symbol)
}

object ClassDiagramElement {

  sealed trait Type extends ClassDiagramElement

  final case class Annotation(displayName: String, symbol: String, isObject: Boolean) extends Type {
    override val isAbstract: Boolean = false

    override def rename(displayName: String): ClassDiagramElement = copy(displayName = displayName)
  }

  final case class Class(displayName: String, symbol: String, isObject: Boolean, isAbstract: Boolean) extends Type {

    override def rename(displayName: String): ClassDiagramElement = copy(displayName = displayName)
  }

  final case class Enum(displayName: String, symbol: String, isObject: Boolean) extends Type {
    override val isAbstract: Boolean = false

    override def rename(displayName: String): ClassDiagramElement = copy(displayName = displayName)
  }

  final case class Interface(displayName: String, symbol: String) extends Type {
    override val isObject: Boolean   = false
    override val isAbstract: Boolean = true

    override def rename(displayName: String): ClassDiagramElement = copy(displayName = displayName)
  }

  sealed trait Visibility

  object Visibility {
    case object Private        extends Visibility
    case object Protected      extends Visibility
    case object PackagePrivate extends Visibility
    case object Public         extends Visibility
  }

  sealed trait Member extends ClassDiagramElement {
    override val isObject: Boolean = false
  }

  final case class Field(displayName: String, symbol: String, visibility: Visibility, isAbstract: Boolean)
      extends Member {

    override def rename(displayName: String): ClassDiagramElement = copy(displayName = displayName)
  }

  final case class Method(
      displayName: String,
      symbol: String,
      visibility: Visibility,
      constructor: Boolean,
      synthetic: Boolean,
      isAbstract: Boolean)
      extends Member {

    override def rename(displayName: String): ClassDiagramElement = copy(displayName = displayName)
  }
}
