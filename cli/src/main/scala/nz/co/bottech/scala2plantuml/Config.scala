package nz.co.bottech.scala2plantuml

import java.io.File
import java.net.URL

final case class Config(
    symbol: String = "",
    includes: Set[String] = Set(),
    excludes: Set[String] = Set(ScalaStdLibPattern, JavaStdLibPattern),
    urls: Vector[URL] = Vector.empty,
    projects: Vector[String] = Vector.empty,
    sourceRoots: Vector[String] = Vector("src/main/scala"),
    outputFile: Option[File] = None,
    logInColour: Boolean = true,
    logLevel: Int = 0) {

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

  def addProject(project: String): Config =
    copy(projects = projects :+ project)

  def addSourceRoot(root: String): Config =
    copy(sourceRoots = sourceRoots :+ root)

  def increaseLogLevel: Config =
    copy(logLevel = logLevel + 1)

  def ignore: String => Boolean = {
    val includesWithDefault = if (includes.isEmpty) Set("**") else includes
    val includeTests        = includesWithDefault.toSeq.map(patternToRegex(_).asMatchPredicate)
    val excludeTests        = excludes.toSeq.map(patternToRegex(_).asMatchPredicate)
    symbol => !includeTests.exists(_.test(symbol)) || excludeTests.exists(_.test(symbol))
  }

  def prefixes: Vector[String] = {
    val projectsWithDefault = if (projects.isEmpty) Vector("") else projects
    for {
      project <- projectsWithDefault
      source  <- sourceRoots
    } yield {
      val suffix = if (project.isEmpty) source else s"$project/$source"
      s"META-INF/semanticdb/$suffix"
    }
  }
}
