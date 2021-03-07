val scala212               = "2.12.13"
val scala213               = "2.13.4"
val supportedScalaVersions = List(scala212, scala213)

val logbackVersion                      = "1.2.3"
val scalaCollectionCompatibilityVersion = "2.3.2"
val scoptVersion                        = "4.0.0"
val slf4jVersion                        = "1.7.30"
val utestVersion                        = "0.7.7"

addCommandAlias(
  "devCheck",
  supportedScalaVersions.flatMap { version =>
    s"++$version" :: "check" :: (if (version == scala213) Nil else List("scripted"))
  }.mkString(";")
)

addCommandAlias(
  "check",
  List(
    "scalafmtCheckAll",
    "scalastyle",
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
    githubWorkflowPublishTargetBranches := List(RefPredicate.StartsWith(Ref.Tag("v"))),
    githubWorkflowTargetTags ++= Seq("v*"),
    pgpSigningKey := Some("0x8DB7DFA142551359!"),
    // This needs to be set otherwise the GitHub workflow plugin gets confused about which
    // version to use for the publish job.
    scalaVersion := scala212,
    versionPolicyFirstVersion := Some("0.1.12"),
    versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
    versionScheme := Some("early-semver")
  )
)

val commonProjectSettings = List(
  isScala213 := isScala213Setting.value,
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
  crossScalaVersions := Nil,
  mimaFailOnNoPrevious := false,
  mimaPreviousArtifacts := Set.empty,
  publish / skip := true
)

val libraryProjectSettings = commonProjectSettings

lazy val root = (project in file("."))
  .aggregate(cli, core, docs, sbtProject)
  .settings(metaProjectSettings)
  .settings(
    crossScalaVersions := supportedScalaVersions,
    name := "scala2plantuml",
    // Workaround for https://github.com/olafurpg/sbt-ci-release/issues/181
    // These have to go on the root project.
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
  )

lazy val core = project
  .settings(libraryProjectSettings)
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
    semanticdbVersion := "4.4.10",
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
    libraryDependencies ++= {
      // Don't add dependencies when the rest of the build is cross building with Scala 2.13
      // otherwise it will cause a whole lot of resolution failures.
      if ((core / isScala213).value) Nil
      else
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
    },
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
    libraryDependencies := libraryDependencies.value.map { module =>
      if (module.name.startsWith("mdoc"))
        module.exclude("io.undertow", "undertow-core").exclude("org.jboss.xnio", "xnio-nio")
      else module
    },
    mdocOut := (ThisBuild / baseDirectory).value,
    mdocVariables := Map(
      // This is configured for GitHub where we want to show the previous version.
      // After a release we need to update the docs and do a git push.
      "VERSION" -> versionPolicyPreviousVersions.value.lastOption.getOrElse(versionPolicyFirstVersion.value.get)
    ),
    unusedCompileDependenciesFilter -= moduleFilter("org.scalameta", "mdoc*")
  )

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
