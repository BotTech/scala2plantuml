package nz.co.bottech.scala2plantuml

import nz.co.bottech.scala2plantuml.SemanticDBLoader._

import java.io.{File, FileInputStream, FilenameFilter, InputStream}
import java.net.URL
import java.nio.file.Paths
import scala.annotation.tailrec
import scala.collection.concurrent.TrieMap
import scala.meta.internal.semanticdb.Scala._
import scala.meta.internal.semanticdb.{TextDocument, TextDocuments}
import scala.util.Using

private[scala2plantuml] class SemanticDBLoader(prefixes: Seq[String], classLoader: ClassLoader) {

  private val cache = TrieMap.empty[String, Result]

  def load(symbol: String): Result = {
    @tailrec
    def loop(paths: Seq[String], errors: Vector[String]): Result =
      paths match {
        case head +: tail =>
          loadPath(head) match {
            case Left(error)  => loop(tail, errors ++ error)
            case Right(value) => Right(value)
          }
        case Seq() => Left(errors)
      }
    semanticdbPath(symbol).flatMap { path =>
      val paths =
        if (prefixes.nonEmpty)
          prefixes.map(prefix => s"$prefix/$path")
        else List(path)
      loop(paths, Vector.empty)
    }
  }

  private def loadPath(path: String): Result =
    loadFilePath(path) match {
      case firstError @ Left(_) =>
        packagePath(path).map(loadDirectoryPath).getOrElse(firstError) match {
          case _ @Left(_) => firstError
          case success    => success
        }
      case right => right
    }

  private def loadFilePath(path: String): Result =
    cache.getOrElseUpdate(
      path,
      getResource(path).flatMap(resource => loadResource(resource.openStream()))
    )

  private def loadDirectoryPath(path: String): Result =
    cache.getOrElseUpdate(
      path,
      getResource(path).flatMap { resource =>
        val file = Paths.get(resource.toURI).toFile
        if (file.isDirectory) {
          val results = findSemanticdbs(file).view.map { file =>
            loadResource(new FileInputStream(file))
          }.takeWhile(_.isRight)
          results.lastOption.collect { case left @ Left(_) => left }.getOrElse {
            Right(results.collect { case Right(textDocuments) => textDocuments }.flatten.toSeq)
          }
        } else
          Left(Vector(s"Cannot load resources from: ${resource.toString}"))
      }
    )

  private def getResource(path: String): Either[Errors, URL] =
    Option(classLoader.getResource(path))
      .toRight(Vector(s"Resource not found: $path"))

  private def loadResource(inputStream: => InputStream): Result =
    Using(inputStream) { semanticdb =>
      TextDocuments.parseFrom(semanticdb).documents
    }.toEither.left.map(error => Vector(error.getLocalizedMessage))

}

private[scala2plantuml] object SemanticDBLoader {

  private type Errors = Vector[String]
  private type Result = Either[Errors, Seq[TextDocument]]

  private[scala2plantuml] def semanticdbPath(symbol: String): Either[Errors, String] =
    if (symbol.isGlobal) {
      val a            = symbol.lastIndexOf('/')
      val (start, end) = symbol.splitAt(a + 1)
      val prefix       = if (start == "_empty" || start == "_root_") "" else start
      val name         = end.takeWhile(c => c != '#' && c != '.')
      if (name == "package") {
        Right(s"${prefix.init}.scala.semanticdb")
      } else Right(s"$prefix$name.scala.semanticdb")
    } else
      Left(Vector(s"Symbol is not global: $symbol"))

  private def packagePath(path: String): Option[String] = {
    val i = path.lastIndexOf('/')
    if (i > 0) Some(path.take(i))
    else None
  }

  private def findSemanticdbs(directory: File): Array[File] =
    directory
      .listFiles(new FilenameFilter {

        override def accept(dir: File, name: String): Boolean =
          name.endsWith(".semanticdb")
      })
}
