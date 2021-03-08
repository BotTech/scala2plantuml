package nz.co.bottech.scala2plantuml

import ch.qos.logback.classic.{Level, Logger}
import nz.co.bottech.scala2plantuml.ClassDiagramRenderer.Options
import org.slf4j.LoggerFactory

import java.io.FileWriter
import java.net.URLClassLoader
import scala.util.Using

@SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
object Scala2PlantUML extends App {

  private val mainThread = Thread.currentThread()

  Runtime.getRuntime.addShutdownHook(new Thread() {

    override def run(): Unit = mainThread.interrupt()
  })

  ConfigParser.parse(args).foreach { config =>
    configureLogging(config)
    val elements = ClassDiagramGenerator.fromSymbol(
      config.symbol,
      config.prefixes,
      config.ignore,
      classLoader(config),
      config.maxLevel
    )
    config.outputFile match {
      case Some(file) =>
        Option(file.getParentFile).foreach(_.mkdirs())
        Using(new FileWriter(file)) { writer =>
          ClassDiagramRenderer.render(elements, Options.Default, writer)
        }.get
      case None =>
        println(ClassDiagramRenderer.renderString(elements, Options.Default)) // scalastyle:ignore
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private def configureLogging(config: Config): Unit = {
    ColourConverter.enabled = config.logInColour
    val root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
    val logLevel = config.logLevel match {
      case 1 => Level.DEBUG
      case 2 => Level.TRACE
      case _ => root.getLevel
    }
    root.setLevel(logLevel)
  }

  private def classLoader(config: Config): ClassLoader =
    new URLClassLoader(config.urls.toArray, getClass.getClassLoader)
}
