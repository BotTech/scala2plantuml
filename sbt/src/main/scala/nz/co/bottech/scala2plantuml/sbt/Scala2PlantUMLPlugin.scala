package nz.co.bottech.scala2plantuml.sbt

import sbt.Keys._
import sbt.plugins.SemanticdbPlugin
import sbt.{Def, _}

object Scala2PlantUMLPlugin extends AutoPlugin {

  override def trigger  = allRequirements
  override def requires = SemanticdbPlugin

  object autoImport {
    val scala2PlantUML = inputKey[File]("Generates a PlantUML class diagram from a base symbol.")
  }

  import autoImport._

  val baseSettings = Seq(
    // Depend on compile so that we can use the SemanticDB files to get the symbol suggestions from
    scala2PlantUML := scala2PlantUMLTask.dependsOn(compile).evaluated,
    scala2PlantUML / sourceDirectories := allSourceDirectories.value,
    scala2PlantUML / fullClasspath += Attributed.blank(semanticdbTargetRoot.value)
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(baseSettings) ++
      inConfig(Test)(baseSettings) ++
      inConfig(Test)(
        scala2PlantUML / sourceDirectories := {
          (Compile / scala2PlantUML / sourceDirectories).value ++
            (scala2PlantUML / sourceDirectories).value
        }
      )

  def scala2PlantUMLTask =
    Def.inputTask {
      val config        = Scala2PlantUMLParser().parsed
      val targetDir     = target.value
      val classpath     = (scala2PlantUML / fullClasspath).value
      val updatedConfig = updateConfig(config, targetDir, classpath)
      val logger        = state.value.log
      if (!semanticdbEnabled.value)
        logger.warn("SemanticDB is not enabled. Set semanticdbEnabled := true.")
      val sourceRoots = (scala2PlantUML / sourceDirectories).value
      if (sourceRoots.exists(_.isAbsolute))
        logger.warn("scala2PlantUML / sourceDirectories should be relative file paths.")
      logger.debug(s"Running Scala2PlantUML with $config")
      Scala2PlantUML(updatedConfig, Seq.empty, sourceRoots.map(_.toString), logger)
    }

  private def updateConfig(config: Config, targetDir: File, classpath: Classpath): Config = {
    val configWithOutputFile =
      if (config.outputFile.isAbsolute) config
      else config.replaceOutputFile(targetDir / config.outputFile.toString)
    classpath.foldLeft(configWithOutputFile) {
      case (config, entry) => config.addFile(entry.data)
    }
  }

  private def allSourceDirectories =
    Def.settingDyn {
      val conf = configuration.value
      Def.setting {
        val dirs = sourceDirectories.all(ScopeFilter(inDependencies(ThisProject), inConfigurations(conf))).value
        val base = (ThisBuild / baseDirectory).value
        dirs.flatten.flatMap(_.relativeTo(base))
      }
    }
}
