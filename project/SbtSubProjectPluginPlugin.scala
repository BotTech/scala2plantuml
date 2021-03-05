import explicitdeps.ExplicitDepsPlugin
import explicitdeps.ExplicitDepsPlugin.autoImport._
import sbt.Keys._
import sbt._
import sbt.plugins.SbtPlugin

// This is all the crazy hacks to get cross compiling working with an sub-project that is an sbt plugin.
object SbtSubProjectPluginPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins      = ExplicitDepsPlugin && SbtPlugin

  private val sspppIsScala213 = settingKey[Boolean]("Checks if the current Scala version is 2.13")

  override def projectSettings: Seq[Def.Setting[_]] =
    List(
      crossScalaVersions := Nil,
      libraryDependencies := libraryDependenciesSetting.value,
      projectDependencies := projectDependenciesTask.value,
      sspppIsScala213 := {
        if (isScala213(scalaVersion.value))
          throw new IllegalStateException("sbt project must not use Scala 2.13. Did you force the version with '+'?")
        isScala213Setting.value
      },
      // We can't skip this as it has to run at least once or sbt complains.
      update / skip := false,
      // Skip everything else otherwise it will just fail.
      skip := sspppIsScala213.value,
      undeclaredCompileDependenciesFilter -= moduleFilter()
    )

  private def isScala213Setting = Def.setting {
    val versions =
      scalaVersion.all(ScopeFilter(inDependencies(ThisProject, transitive = true, includeRoot = false))).value
    versions.exists(isScala213)
  }

  private def isScala213(version: String) =
    CrossVersion.partialVersion(version) match {
      case Some((2, n)) if n == 13 => true
      case _                       => false
    }

  private def projectDependenciesTask = Def.task {
    // Remove all project dependencies for Scala 2.13 as they will not resolve when cross building.
    if (sspppIsScala213.value) {
      Seq.empty
    } else {
      projectDependencies.value
    }
  }

  private def libraryDependenciesSetting = Def.setting {
    // Remove all library dependencies for Scala 2.13 as they will not resolve when cross building.
    if (sspppIsScala213.value) {
      Seq.empty
    } else {
      libraryDependencies.value
    }
  }
}
