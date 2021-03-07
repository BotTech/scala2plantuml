import ConditionalKeys._
import explicitdeps.ExplicitDepsPlugin.autoImport._
import sbt.Keys._
import sbt.ScriptedPlugin.autoImport.scripted
import sbt.plugins.SbtPlugin
import sbt.{Def, _}

// This is all the crazy hacks to get cross compiling working with an sub-project that is an sbt plugin.
object SbtPluginSubProjectPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins      = SbtPlugin

  object autoImport {
    val spspIsSbtCompatibleScalaVersion = settingKey[Boolean]("Checks if the current Scala version is 2.13")
  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] =
    List(
      crossScalaVersions := Nil,
      // Remove all library dependencies for Scala 2.13 as they will not resolve when cross building.
      libraryDependencies := settingDefaultIfSetting(libraryDependencies, scalaVersion, isScala213, Nil).value,
      // Remove all project dependencies for Scala 2.13 as they will not resolve when cross building.
      projectDependencies := taskDefaultIfSkipped(projectDependencies, Nil).value,
      scripted := inputDefaultIfSkipped(scripted, ()).evaluated,
      spspIsSbtCompatibleScalaVersion := isSbtCompatibleScalaVersionSetting.value,
      // We can't skip this as it has to run at least once or sbt complains.
      update / skip := false,
      // Skip everything else otherwise it will just fail.
      skip := !spspIsSbtCompatibleScalaVersion.value,
      undeclaredCompileDependenciesFilter -= moduleFilter()
    )

  private def isSbtCompatibleScalaVersionSetting = Def.setting {
    if (isScala213(scalaVersion.value))
      throw new IllegalStateException("sbt project must not use Scala 2.13. Did you force the version with '+'?")
    val versions =
      scalaVersion.all(ScopeFilter(inDependencies(ThisProject, transitive = true, includeRoot = false))).value
    !versions.exists(isScala213)
  }

  private def isScala213(version: String) =
    CrossVersion.partialVersion(version) match {
      case Some((2, n)) if n == 13 => true
      case _                       => false
    }
}
