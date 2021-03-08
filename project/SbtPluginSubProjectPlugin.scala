import ConditionalKeys._
import com.typesafe.tools.mima.plugin.MimaKeys.mimaPreviousArtifacts
import explicitdeps.ExplicitDepsPlugin
import explicitdeps.ExplicitDepsPlugin.autoImport._
import sbt.Keys._
import sbt.ScriptedPlugin.autoImport.scripted
import sbt._
import sbt.plugins.SbtPlugin
import sbtversionpolicy.SbtVersionPolicyMima

// This is all the crazy hacks to get cross compiling working with an sub-project that is an sbt plugin.
object SbtPluginSubProjectPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins      = SbtPlugin && SbtVersionPolicyMima && ExplicitDepsPlugin

  object autoImport {
    val spspCanBuild = settingKey[Boolean]("Checks if the project dependencies are using a compatible Scala version.")
  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] =
    List(
      crossScalaVersions := Nil,
      // Remove all project dependencies for Scala 2.13 as they will not resolve when cross building.
      projectDependencies := taskDefaultIfSkipped(projectDependencies, Nil).value,
      scripted := inputDefaultIfSkipped(scripted, ()).evaluated,
      spspCanBuild := canBuildSetting.value,
      // We can't skip this as it has to run at least once or sbt complains.
      update / skip := false,
      // Skip everything else otherwise it will just fail.
      skip := !spspCanBuild.value,
      undeclaredCompileDependenciesFilter -= moduleFilter(),
      mimaPreviousArtifacts := defaultIfCannotBuild(mimaPreviousArtifacts, Set.empty[ModuleID]).value
    )

  def defaultIfCannotBuild[A](setting: Def.Initialize[A], default: => A): Def.Initialize[A] =
    settingDefaultIfSetting(setting, spspCanBuild, default)(!_)

  private def canBuildSetting = Def.setting {
    if (!isScala212(scalaVersion.value))
      throw new IllegalStateException(
        "sbt project must use Scala 2.12. Check that you have not forced the version with '+'."
      )
    val versions =
      scalaVersion.all(ScopeFilter(inDependencies(ThisProject, transitive = true, includeRoot = false))).value
    versions.forall(isScala212)
  }

  private def isScala212(version: String) =
    CrossVersion.partialVersion(version) match {
      case Some((2, n)) if n == 12 => true
      case _                       => false
    }
}
