import com.typesafe.tools.mima.core._

val scala212               = "2.12.13"
val scala213               = "2.13.14"
val supportedScalaVersions = List(scala212, scala213)

val logbackVersion                      = "1.2.3"
val scalaCollectionCompatibilityVersion = "2.4.2"
val scoptVersion                        = "4.0.1"
val sdbVersion                          = "4.4.10"
val slf4jVersion                        = "1.7.30"
val utestVersion                        = "0.7.7"

addCommandAlias(
  "devCheck",
  supportedScalaVersions.flatMap { version =>
    s"++$version" :: "githubWorkflowCheck" :: "check" :: (if (version == scala213) Nil else List("scripted"))
  }.mkString(";")
)

addCommandAlias(
  "check",
  List(
    "scalafmtCheckAll",
    "scalastyle",
    "versionCheck",
    "versionPolicyCheck",
    "docs/mdoc --check",
    "evicted",
    "undeclaredCompileDependenciesTest",
    "unusedCompileDependenciesTest",
    "dependencyCheckAggregate",
    "test"
  ).mkString(";")
)

val isScala213 = settingKey[Boolean]("Checks if the current Scala version is 2.13")

inThisBuild(
  List(
    crossScalaVersions := supportedScalaVersions,
    dependencyCheckAssemblyAnalyzerEnabled := Some(false),
    description := "Scala2PlantUML generates PlantUML diagrams from Scala code.",
    // We have to have this otherwise the release fails.
    developers += Developer(
      "steinybot",
      "Jason Pickens",
      "jasonpickensnz@gmail.com",
      url("https://github.com/steinybot")
    ),
    // TODO: Add this when sbt 1.5.0 is released.
    // evictionErrorLevel := Level.Error,
    homepage := Some(url("https://github.com/BotTech/scala2plantuml")),
    licenses := List("MIT" -> url("https://github.com/BotTech/scala2plantuml/blob/main/LICENSE")),
    organization := "nz.co.bottech",
    organizationName := "BotTech",
    githubWorkflowBuild := List(
      WorkflowStep.Sbt(List("check"), name = Some("Build, test and check libraries")),
      WorkflowStep.Sbt(
        List("scripted"),
        name = Some("Build and test sbt plugin"),
        cond = Some(s"""matrix.scala == '$scala212'""")
      )
    ),
    // The first of these is the version for the publish job which has to be 9+
    // because of https://github.com/xerial/sbt-sonatype/issues/216
    githubWorkflowJavaVersions := List("adopt@1.11", "adopt@1.8"),
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
    // TODO: #17 - Fix main snapshot release.
    //githubWorkflowPublishTargetBranches += RefPredicate.StartsWith(Ref.Tag("v")),
    githubWorkflowPublishTargetBranches := List(RefPredicate.StartsWith(Ref.Tag("v"))),
    githubWorkflowTargetTags ++= List("v*"),
    pgpSigningKey := Some("0x8DB7DFA142551359!"),
    // This needs to be set otherwise the GitHub workflow plugin gets confused about which
    // version to use for the publish job.
    scalaVersion := scala212,
    versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
    versionScheme := Some("early-semver")
  )
)

val commonProjectSettings = List(
  isScala213 := isScala213Setting.value,
  name := s"${(LocalRootProject / name).value}-${name.value}",
  scalastyleFailOnError := true,
  scalastyleFailOnWarning := true,
  // Workaround for https://github.com/cb372/sbt-explicit-dependencies/issues/97
  undeclaredCompileDependenciesFilter -= moduleFilter("com.thesamet.scalapb", "scalapb-runtime"),
  Compile / compile / wartremoverErrors := {
    // Workaround for https://github.com/wartremover/wartremover/issues/504
    if (isScala213.value) Warts.unsafe.filterNot(_.clazz == Wart.Any.clazz)
    else Warts.unsafe
  },
  Test / compile / wartremoverErrors := {
    (Compile / compile / wartremoverErrors).value.filterNot(_.clazz == Wart.DefaultArguments.clazz)
  }
)

val metaProjectSettings = List(
  mimaFailOnNoPrevious := false,
  mimaPreviousArtifacts := Set.empty,
  publish / skip := true,
  versionPolicyCheck := Def.unit(())
)

lazy val root = (project in file("."))
  .aggregate(cli, core, docs, example, integrationTests, sbtProject)
  .settings(metaProjectSettings)
  .settings(
    name := "scala2plantuml",
    // Workaround for https://github.com/olafurpg/sbt-ci-release/issues/181
    // These have to go on the root project.
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
  )

lazy val core = project
  .settings(commonProjectSettings)
  .settings(
    libraryDependencies ++= collectionsCompatibilityDependency.value,
    libraryDependencies ++= List(
      "org.scalameta" %% "scalameta"       % semanticdbVersion.value,
      "org.scalameta" %% "trees"           % semanticdbVersion.value,
      "org.slf4j"      % "slf4j-api"       % slf4jVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion % Test,
      "ch.qos.logback" % "logback-core"    % logbackVersion % Test,
      "com.lihaoyi"   %% "utest"           % utestVersion   % Test
    ),
    name := s"${(LocalRootProject / name).value}",
    semanticdbEnabled := true,
    semanticdbVersion := sdbVersion,
    testFrameworks += new TestFramework("utest.runner.Framework"),
    Test / managedSourceDirectories += (Test / semanticdbTargetRoot).value,
    Test / fullClasspath += (Test / semanticdbTargetRoot).value
  )

lazy val cli = project
  .dependsOn(core)
  .enablePlugins(BuildInfoPlugin)
  .settings(commonProjectSettings)
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
    mimaForwardIssueFilters += "0.2.0" -> List(
      // This is private so no harm done.
      ProblemFilters.exclude[MissingClassProblem]("nz.co.bottech.scala2plantuml.ConfigParser$Terminated$")
    )
  )

// Use a separate project rather than a configuration to get IntelliJ support.
lazy val integrationTests = (project in (file("meta") / "integration-tests"))
  .settings(metaProjectSettings)
  .settings(
    libraryDependencies ++= List(
      "com.lihaoyi" %% "utest" % utestVersion % Test
    ),
    testFrameworks += new TestFramework("utest.runner.Framework"),
    Test / fork := true,
    Test / javaOptions += s"-Dit.classpath=${(cli / Compile / fullClasspathAsJars).value.map(_.data).mkString(":")}"
  )

lazy val sbtProject = (project in file("sbt"))
  .dependsOn(core)
  .enablePlugins(SbtPlugin)
  .settings(commonProjectSettings)
  .settings(
    libraryDependencies ++= {
      // Only add dependencies when the build is building with a Scala version that is
      // sbt compatible otherwise it will cause a whole lot of resolution failures.
      if (spspCanBuild.value)
        collectionsCompatibilityDependency.value ++ List(
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
        )
      else Nil
    },
    name := s"sbt-${(LocalRootProject / name).value}",
    scriptedBufferLog := false,
    // TODO: Remove this once https://github.com/sbt/sbt/pull/6351 is released.
    scriptedDependencies := {
      Def.unit(scriptedDependencies.value)
      Def.unit(publishLocal.all(ScopeFilter(projects = inDependencies(ThisProject, includeRoot = false))).value)
    },
    scriptedLaunchOpts += s"-Dplugin.version=${version.value}"
  )

lazy val docs = (project in (file("meta") / "docs"))
  // Include build info here so that we can override the version.
  .enablePlugins(BuildInfoPlugin, MdocPlugin)
  .dependsOn(cli, example)
  .settings(metaProjectSettings)
  .settings(
    // We use a different version setting so that it may depend on versionPolicyPreviousVersions
    // without creating a cycle. This means that we need to map the identifier back so that it
    // matches the same key used when compiling the build info for the CLI.
    buildInfoKeys := Seq[BuildInfoKey](BuildInfoKey.map(mdoc / version)({ case (_, value) => "version" -> value })),
    buildInfoPackage := s"${organization.value}.${(LocalRootProject / name).value}",
    libraryDependencies := libraryDependencies.value.map { module =>
      if (module.name.startsWith("mdoc"))
        module.exclude("io.undertow", "undertow-core").exclude("org.jboss.xnio", "xnio-nio")
      else module
    },
    mdocOut := (ThisBuild / baseDirectory).value,
    mdocVariables := Map(
      "VERSION"       -> (mdoc / version).value,
      "SCALA_VERSION" -> scalaMajorMinorVersion.value
    ),
    unusedCompileDependenciesFilter -= moduleFilter("org.scalameta", "mdoc*"),
    mdoc / version := versionPolicyPreviousVersions.value.lastOption.getOrElse(version.value)
  )

lazy val example = project
  .settings(commonProjectSettings)
  .settings(
    semanticdbEnabled := true,
    semanticdbIncludeInJar := true,
    semanticdbVersion := sdbVersion,
    versionPolicyCheck := Def.unit(())
  )

def scalaMajorMinorVersion: Def.Initialize[String] = Def.setting {
  CrossVersion.partialVersion(scala213) match {
    case Some((major, minor)) => s"$major.$minor"
    case _                    => throw new IllegalArgumentException("scalaVersion is malformed.")
  }
}

def isScala213Setting: Def.Initialize[Boolean] = Def.setting {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, n)) if n == 13 => true
    case _                       => false
  }
}

def collectionsCompatibilityDependency: Def.Initialize[List[ModuleID]] = Def.setting {
  if (isScala213.value) Nil
  else List("org.scala-lang.modules" %% "scala-collection-compat" % scalaCollectionCompatibilityVersion)
}
