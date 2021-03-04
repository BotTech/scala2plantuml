package nz.co.bottech.scala2plantuml

import scala.meta.internal.semanticdb.SymbolInformation

private[scala2plantuml] object AdditionalSymbolInformation {

  def isAnnotation(symbolInformation: SymbolInformation, typeIndex: TypeIndex): Boolean =
    subTypeOf(symbolInformation, "scala/annotation/Annotation#", typeIndex)

  def isEnum(symbolInformation: SymbolInformation, typeIndex: TypeIndex): Boolean =
    subTypeOf(symbolInformation, "scala/Enumeration#", typeIndex) ||
      subTypeOf(symbolInformation, "java/lang/Enum#", typeIndex)

  def subTypeOf(symbolInformation: SymbolInformation, parent: String, typeIndex: TypeIndex): Boolean = {
    val hierarchy = typeIndex.hierarchy(symbolInformation)
    hierarchy.subTypeOf(parent)
  }

  def isObject(symbolInformation: SymbolInformation): Boolean =
    symbolInformation.kind == SymbolInformation.Kind.OBJECT

  def isTrait(symbolInformation: SymbolInformation): Boolean =
    symbolInformation.kind == SymbolInformation.Kind.TRAIT

  def isConstructor(symbolInformation: SymbolInformation): Boolean =
    symbolInformation.kind == SymbolInformation.Kind.CONSTRUCTOR

  def isAbstract(symbolInformation: SymbolInformation): Boolean =
    hasProperty(symbolInformation, SymbolInformation.Property.ABSTRACT)

  def isField(symbolInformation: SymbolInformation): Boolean =
    isVal(symbolInformation) || isVar(symbolInformation)

  def isVal(symbolInformation: SymbolInformation): Boolean =
    hasProperty(symbolInformation, SymbolInformation.Property.VAL)

  def isVar(symbolInformation: SymbolInformation): Boolean =
    hasProperty(symbolInformation, SymbolInformation.Property.VAR)

  def hasProperty(symbolInformation: SymbolInformation, property: SymbolInformation.Property): Boolean =
    (symbolInformation.properties & property.value) == property.value
}
