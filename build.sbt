ThisBuild / organization := "nz.co.bottech"
ThisBuild / organizationName := "BotTech"
ThisBuild / homepage := Some(url("https://github.com/BotTech/scala2plantuml"))
ThisBuild / version := "0.1"

name := "scala2plantuml"
crossScalaVersions := Nil
publish / skip := true

aggregateProjects(cli, core, sbtPluginProject)

val scala212               = "2.12.13"
val scala213               = "2.13.4"
val supportedScalaVersions = List(scala212)

val logbackVersion = "1.2.3"
val scoptVersion   = "4.0.0"
val slf4jVersion   = "1.7.30"
val utestVersion   = "0.7.7"

val commonLibraryProjectSettings = List(
  scalaVersion := scala212,
  crossScalaVersions := supportedScalaVersions
)

lazy val core = project
  .settings(commonLibraryProjectSettings: _*)
  .settings(
    name := s"${(LocalRootProject / name).value}",
    // Required for testing.
    semanticdbEnabled := true,
    semanticdbVersion := "4.4.10",
    libraryDependencies ++= List(
      "org.scalameta" %% "scalameta"       % semanticdbVersion.value,
      "org.scalameta" %% "trees"           % semanticdbVersion.value,
      "org.slf4j"      % "slf4j-api"       % slf4jVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion % Test,
      "ch.qos.logback" % "logback-core"    % logbackVersion % Test,
      "com.lihaoyi"   %% "utest"           % utestVersion   % Test
    ),
    testFrameworks += new TestFramework("utest.runner.Framework"),
    Test / managedSourceDirectories += (Test / semanticdbTargetRoot).value,
    Test / fullClasspath += (Test / semanticdbTargetRoot).value
  )

lazy val cli = project
  .dependsOn(core)
  .enablePlugins(BuildInfoPlugin)
  .settings(commonLibraryProjectSettings: _*)
  .settings(
    name := s"${(LocalRootProject / name).value}-cli",
    libraryDependencies ++= List(
      "ch.qos.logback"    % "logback-classic" % logbackVersion,
      "ch.qos.logback"    % "logback-core"    % logbackVersion,
      "com.github.scopt" %% "scopt"           % scoptVersion
    ),
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := s"${organization.value}.${(LocalRootProject / name).value}"
  )

lazy val sbtPluginProject = (project in file("sbt"))
  .dependsOn(core)
  .enablePlugins(SbtPlugin)
  .settings(
    name := s"sbt-${(LocalRootProject / name).value}"
  )
