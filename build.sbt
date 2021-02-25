ThisBuild / organization := "nz.co.bottech"
ThisBuild / organizationName := "BotTech"
ThisBuild / homepage := Some(url("https://github.com/BotTech/scala2plantuml"))
ThisBuild / version := "0.1"

name := "scala2plantuml"
crossScalaVersions := Nil
publish / skip := true

aggregateProjects(core)

val scala212 = "2.12.13"
val scala213 = "2.13.4"
val supportedScalaVersions = List(scala212, scala213)

val commonLibraryProjectSettings = List(
  crossScalaVersions := supportedScalaVersions
)

lazy val core = project
  .settings(commonLibraryProjectSettings: _*)
  .settings(
    name := s"${(LocalRootProject / name).value}",
    // Required for testing.
    semanticdbEnabled := true,
    semanticdbVersion := "4.4.9",
    libraryDependencies ++= List(
      "org.scalameta" %% "scalameta"       % semanticdbVersion.value,
      "org.scalameta" %% "trees"           % semanticdbVersion.value,
      "org.slf4j"      % "slf4j-api"       % "1.7.30",
      "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
      "ch.qos.logback" % "logback-core"    % "1.2.3" % Test,
      "com.lihaoyi"   %% "utest"           % "0.7.7" % Test
    ),
    testFrameworks += new TestFramework("utest.runner.Framework"),
    Test / fullClasspath += (Test / semanticdbTargetRoot).value
  )
