package nz.co.bottech.scala2plantuml.sbt

import nz.co.bottech.scala2plantuml.ClassDiagramRenderer.Options
import nz.co.bottech.scala2plantuml.{ClassDiagramGenerator, ClassDiagramRenderer}
import sbt.util.Logger

import java.io.{File, FileWriter}
import java.net.URLClassLoader
import scala.util.Using

object Scala2PlantUML {

  def apply(config: Config, projects: Seq[String], sourceRoots: Seq[String], logger: Logger): File = {
    validateConfig(config)
    configureLogging(config, logger)
    val elements = ClassDiagramGenerator.fromSymbol(
      config.symbol,
      config.prefixes(projects, sourceRoots),
      config.ignore,
      classLoader(config)
    )
    val file = config.outputFile
    Option(file.getParentFile).foreach(_.mkdirs())
    Using(new FileWriter(file)) { writer =>
      ClassDiagramRenderer.render(elements, Options.Default, writer)
    }.get
    file
  }

  private def validateConfig(config: Config): Unit =
    if (config.ignore(config.symbol))
      throw new IllegalArgumentException("Symbol must match include patterns and not match exclude patterns.")

  private def configureLogging(config: Config, logger: Logger): Unit = {
    // TODO
    //    val root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    //    root.
    val _ = (config, logger)
  }

  private def classLoader(config: Config): ClassLoader =
    new URLClassLoader(config.urls.toArray, getClass.getClassLoader)
}
