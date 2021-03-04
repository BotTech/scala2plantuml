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
      Compile / classpathConfiguration := Compile,
      Runtime / classpathConfiguration := Runtime,
      Test / classpathConfiguration := Test,
      skip := {
        val versions =
          scalaVersion.all(ScopeFilter(inDependencies(ThisProject, transitive = false, includeRoot = false))).value
        versions.exists { version =>
          CrossVersion.partialVersion(version) match {
            case Some((2, n)) if n == 13 => true
            case _                       => false
          }
        }
      },
      update := {
        if (skip.value) dummyUpdateReport
        else update.value
      }
    )

  private def dummyUpdateReport =
    UpdateReport(new File("."), Vector.empty, UpdateStats(-1L, -1L, -1L, cached = false), Map.empty)
}
