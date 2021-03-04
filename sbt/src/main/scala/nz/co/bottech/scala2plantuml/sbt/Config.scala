package nz.co.bottech.scala2plantuml.sbt

import nz.co.bottech.scala2plantuml._

import java.io.File
import java.net.URL

// TODO: Refactor the common code between here and the CLI.
final case class Config(
    symbol: String,
    outputFile: File,
    includes: Set[String] = Set(),
    excludes: Set[String] = Set(ScalaStdLibPattern, JavaStdLibPattern),
    urls: Vector[URL] = Vector.empty) {

  def replaceOutputFile(file: String): Config =
    replaceOutputFile(new File(file))

  def replaceOutputFile(file: File): Config =
    copy(outputFile = file)

  def addInclude(pattern: String): Config =
    copy(includes = includes + pattern)

  def addExclude(pattern: String): Config =
    copy(excludes = excludes + pattern)

  def addDirectory(directory: File): Config = {
    // The directory has to end with a trailing slash to be loaded as a directory.
    val directoryWithSlash =
      if (directory.toString.endsWith("/")) directory
      else new File(directory, "/")
    addFile(directoryWithSlash)
  }

  def addFile(file: File): Config =
    addURL(file.toURI.toURL)

  def addURL(url: URL): Config =
    copy(urls = urls :+ url)

  def ignore: String => Boolean = {
    val includesWithDefault = if (includes.isEmpty) Set("**") else includes
    val includeTests        = includesWithDefault.toSeq.map(patternPredicate)
    val excludeTests        = excludes.toSeq.map(patternPredicate)
    symbol => !includeTests.exists(_(symbol)) || excludeTests.exists(_(symbol))
  }

  def prefixes(projects: Seq[String], sourceRoots: Seq[String]): Seq[String] = {
    val projectsWithDefault = if (projects.isEmpty) Seq("") else projects
    for {
      project <- projectsWithDefault
      source  <- sourceRoots
    } yield {
      val suffix = if (project.isEmpty) source else s"$project/$source"
      s"META-INF/semanticdb/$suffix"
    }
  }
}
