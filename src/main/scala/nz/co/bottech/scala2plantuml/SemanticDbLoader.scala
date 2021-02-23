package nz.co.bottech.scala2plantuml

import scala.annotation.tailrec
import scala.meta.internal.semanticdb.Scala._
import scala.meta.internal.semanticdb.{TextDocument, TextDocuments}
import scala.util.Using

class SemanticDbLoader(prefixes: Seq[String], classLoader: ClassLoader) {

  private val prefixesOrEmpty = {
    if (prefixes.isEmpty) List("") else prefixes
  }

  def load(symbol: String): Either[String, Seq[TextDocument]] = {
    @tailrec
    def loop(path: String, remaining: Seq[String], errors: Vector[String]): Either[Vector[String], Seq[TextDocument]] =
      remaining match {
        case prefix +: tail =>
          val pathToLoad = if (prefix.isEmpty) path else s"$prefix/$path"
          loadPath(pathToLoad) match {
            case Left(error)  => loop(path, tail, errors :+ error)
            case Right(value) => Right(value)
          }
        case Seq() => Left(errors)
      }
    symbolSemanticDbPath(symbol).flatMap { path =>
      loop(path, prefixesOrEmpty, Vector.empty).left.map(_.mkString("\n"))
    }
  }

  private def loadPath(path: String): Either[String, Seq[TextDocument]] =
    Option(classLoader.getResourceAsStream(path))
      .toRight(s"Resource not found: $path")
      .flatMap(Using(_) { semanticDb =>
        TextDocuments.parseFrom(semanticDb).documents
      }.toEither.left.map(_.getMessage))

  private def symbolSemanticDbPath(symbol: String): Either[String, String] =
    // TODO: This needs to be way more robust.
    if (symbol.isGlobal)
      Right(s"${symbol.dropRight(1)}.scala.semanticdb")
    else
      // TODO: Is this true? What does that actually mean?
      Left("Symbol must be a global symbol.")
}
