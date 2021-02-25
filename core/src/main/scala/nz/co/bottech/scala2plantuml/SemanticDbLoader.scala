package nz.co.bottech.scala2plantuml

import java.io.{File, FileInputStream, FilenameFilter, InputStream}
import java.net.URL
import java.nio.file.Paths
import scala.annotation.tailrec
import scala.collection.concurrent.TrieMap
import scala.meta.internal.semanticdb.Scala._
import scala.meta.internal.semanticdb.{TextDocument, TextDocuments}
import scala.util.Using

private[scala2plantuml] class SemanticdbLoader(prefixes: Seq[String], classLoader: ClassLoader) {

  type Result = Either[String, Seq[TextDocument]]

  private val cache = TrieMap.empty[String, Result]

  def load(symbol: String): Result = {
    @tailrec
    def loop(paths: Seq[String], errors: Vector[String]): Either[Vector[String], Seq[TextDocument]] =
      paths match {
        case head +: tail =>
          loadPath(head) match {
            case Left(error)  => loop(tail, errors :+ error)
            case Right(value) => Right(value)
          }
        case Seq() => Left(errors)
      }
    semanticdbPath(symbol).flatMap { path =>
      val paths =
        if (prefixes.nonEmpty)
          prefixes.map(prefix => s"$prefix/$path")
        else List(path)
      loop(paths, Vector.empty).left.map(_.mkString("\n"))
    }
  }

  private def loadPath(path: String): Result =
    loadFilePath(path) match {
      case left @ Left(_) =>
        packagePath(path).map(loadDirectoryPath).getOrElse(left)
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
          results.lastOption.collect {
            case left @ Left(_) => left
          }.getOrElse {
            Right(results.collect {
              case Right(textDocuments) => textDocuments
            }.flatten.toSeq)
          }
        } else
          Left(s"Cannot load resources within $resource")
      }
    )

  private def getResource(path: String): Either[String, URL] =
    Option(classLoader.getResource(path))
      .toRight(s"Resource not found: $path")

  private def loadResource(inputStream: => InputStream): Result =
    Using(inputStream) { semanticdb =>
      TextDocuments.parseFrom(semanticdb).documents
    }.toEither.left.map(_.getMessage)

  private def semanticdbPath(symbol: String): Either[String, String] =
    if (symbol.isGlobal)
      Right(s"${symbol.dropRight(1).takeWhile(_ != '#')}.scala.semanticdb")
    else
      Left("Symbol must be a global symbol.")

  private def packagePath(path: String): Option[String] = {
    val i = path.lastIndexOf('/')
    if (i > 0) Some(path.take(i))
    else None
  }

  private def findSemanticdbs(directory: File): Array[File] =
    directory.listFiles(new FilenameFilter {

      override def accept(dir: File, name: String): Boolean =
        name.endsWith(".semanticdb")
    })
}
