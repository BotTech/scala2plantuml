import sbt.Keys._
import sbt._
import sbt.plugins.SbtPlugin

// This is all the crazy hacks to get cross compiling working with an sub-project that is an sbt plugin.
object SbtSubProjectPluginPlugin extends AutoPlugin {

  override def trigger  = allRequirements
  override def requires = SbtPlugin

  override def projectSettings: Seq[Def.Setting[_]] =
    List(
      crossScalaVersions := Nil,
      evicted := evictedTask.value,
      skip := skipTask.value,
      update := updateTask.value
    )

  private def evictedTask = Def.task {
    val report = update.value
    EvictionWarning(ivyModule.value, (evicted / evictionWarningOptions).value, report)
  }

  private def skipTask =
    Def.task {
      val versions =
        scalaVersion.all(ScopeFilter(inDependencies(ThisProject, transitive = false, includeRoot = false))).value
      versions.exists { version =>
        CrossVersion.partialVersion(version) match {
          case Some((2, n)) if n == 13 => true
          case _                       => false
        }
      }
    }

  //noinspection MutatorLikeMethodIsParameterless
  private def updateTask =
    Def.task {
      if (skip.value) {
        val configurations = ivyModule.value.configurations
        dummyUpdateReport(configurations)
      } else update.value
    }

  private def dummyUpdateReport(configurations: Vector[Configuration]) =
    UpdateReport(
      new File("."),
      configurations.map(dummyConfigurationReport),
      UpdateStats(-1L, -1L, -1L, cached = false),
      Map.empty
    )

  private def dummyConfigurationReport(configuration: Configuration) =
    ConfigurationReport(configuration, Vector.empty, Vector.empty)

}
