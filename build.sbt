import sbt.Def

val scala212               = "2.12.13"
val scala213               = "2.13.4"
val supportedScalaVersions = List(scala212, scala213)

val logbackVersion                      = "1.2.3"
val scalaCollectionCompatibilityVersion = "2.3.2"
val scoptVersion                        = "4.0.0"
val slf4jVersion                        = "1.7.30"
val utestVersion                        = "0.7.7"

addCommandAlias(
  "check",
  List(
    "scalafmtCheckAll",
    "scalastyle",
    "+undeclaredCompileDependenciesTest",
    "+unusedCompileDependenciesTest",
    "+dependencyCheckAggregate",
    "+mimaReportBinaryIssues",
    "+test",
    "scripted"
  ).mkString("; ")
)

inThisBuild(
  List(
    crossScalaVersions := supportedScalaVersions,
    description := "Scala2PlantUML generates PlantUML diagrams from Scala code.",
    homepage := Some(url("https://github.com/BotTech/scala2plantuml")),
    licenses := List("MIT" -> url("https://github.com/BotTech/scala2plantuml/blob/main/LICENSE")),
    organization := "nz.co.bottech",
    organizationName := "BotTech",
    githubWorkflowBuild := List(
      WorkflowStep.Sbt(List("scalafmtCheckAll", "scalastyle"), name = Some("Check formatting and style")),
      WorkflowStep.Sbt(List("doc/mdoc"), name = Some("Check documentation has been generated")),
      WorkflowStep.Sbt(
        List("undeclaredCompileDependenciesTest", "unusedCompileDependenciesTest"),
        name = Some("Check declared dependencies")
      ),
      WorkflowStep.Sbt(List("dependencyCheckAggregate"), name = Some("Check for known vulnerabilities")),
      WorkflowStep.Sbt(List("mimaReportBinaryIssues"), name = Some("Check binary compatibility")),
      WorkflowStep.Sbt(List("test", "scripted"), name = Some("Build and test"))
    ),
    githubWorkflowPublish := List(
      WorkflowStep.Sbt(
        List("ci-release"),
        env = Map(
          "PGP_PASSPHRASE"    -> "${{ secrets.PGP_PASSPHRASE }}",
          "PGP_SECRET"        -> "${{ secrets.PGP_SECRET }}",
          "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
          "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
        )
      )
    ),
    githubWorkflowPublishTargetBranches := List(RefPredicate.StartsWith(Ref.Tag("v"))),
    githubWorkflowTargetTags ++= Seq("v*")
  )
)

val commonProjectSettings = List(
  mimaPreviousArtifacts := previousStableVersion.value.map(organization.value %% moduleName.value % _).toSet,
  scalastyleFailOnError := true,
  scalastyleFailOnWarning := true,
  // Workaround for https://github.com/cb372/sbt-explicit-dependencies/issues/97
  undeclaredCompileDependenciesFilter -= moduleFilter("com.thesamet.scalapb", "scalapb-runtime"),
  wartremoverErrors ++= Warts.unsafe
)

val metaProjectSettings = List(
  crossScalaVersions := Nil,
  publish / skip := true,
  mimaFailOnNoPrevious := false
)

val libraryProjectSettings = commonProjectSettings

lazy val root = (project in file("."))
  .aggregate(cli, core, docs, sbtProject)
  .settings(metaProjectSettings)
  .settings(
    crossScalaVersions := supportedScalaVersions,
    name := "scala2plantuml"
  )

lazy val core = project
  .settings(libraryProjectSettings)
  .settings(
    name := s"${(LocalRootProject / name).value}",
    semanticdbEnabled := true,
    semanticdbVersion := "4.4.10",
    libraryDependencies ++= collectionsCompatibilityDependency.value,
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
  .settings(libraryProjectSettings)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := s"${organization.value}.${(LocalRootProject / name).value}",
    libraryDependencies ++= collectionsCompatibilityDependency.value,
    libraryDependencies ++= List(
      "ch.qos.logback"    % "logback-classic" % logbackVersion,
      "ch.qos.logback"    % "logback-core"    % logbackVersion,
      "com.github.scopt" %% "scopt"           % scoptVersion,
      "org.slf4j"         % "slf4j-api"       % slf4jVersion
    ),
    name := s"${(LocalRootProject / name).value}-cli"
  )

lazy val sbtProject = (project in file("sbt"))
  .dependsOn(core)
  .enablePlugins(SbtPlugin)
  .settings(commonProjectSettings)
  .settings(
    libraryDependencies ++= collectionsCompatibilityDependency.value,
    libraryDependencies ++= List(
      "org.scala-sbt" %% "collections"            % sbtVersion.value,
      "org.scala-sbt" %% "command"                % sbtVersion.value,
      "org.scala-sbt"  % "compiler-interface"     % "1.4.4",
      "org.scala-sbt" %% "completion"             % sbtVersion.value,
      "org.scala-sbt" %% "core-macros"            % sbtVersion.value,
      "org.scala-sbt" %% "io"                     % "1.4.0",
      "org.scala-sbt" %% "librarymanagement-core" % "1.4.3",
      "org.scala-sbt" %% "main"                   % sbtVersion.value,
      "org.scala-sbt" %% "main-settings"          % sbtVersion.value,
      "org.scala-sbt"  % "sbt"                    % sbtVersion.value,
      "org.scala-sbt" %% "task-system"            % sbtVersion.value,
      "org.scala-sbt" %% "util-logging"           % sbtVersion.value,
      "org.scala-sbt" %% "util-position"          % sbtVersion.value
    ),
    name := s"sbt-${(LocalRootProject / name).value}",
    scriptedBufferLog := false,
    scriptedDependencies := {
      Def.unit(scriptedDependencies.value)
      Def.unit(publishLocal.all(ScopeFilter(projects = inDependencies(ThisProject, includeRoot = false))).value)
    },
    scriptedLaunchOpts += s"-Dplugin.version=${version.value}"
  )

lazy val docs = (project in file("doc-templates"))
  .enablePlugins(MdocPlugin)
  .settings(metaProjectSettings)
  .settings(
    mdocExtraArguments ++= {
      if (githubIsWorkflowBuild.value) List("--check")
      else Nil
    },
    mdocOut := (ThisBuild / baseDirectory).value,
    mdocVariables := Map(
      "VERSION" -> version.value
    )
  )

def collectionsCompatibilityDependency: Def.Initialize[List[ModuleID]] = Def.setting {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, n)) if n == 12 =>
      List("org.scala-lang.modules" %% "scala-collection-compat" % scalaCollectionCompatibilityVersion)
    case _ => Nil
  }
}
