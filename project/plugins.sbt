addSbtPlugin("ch.epfl.scala"             % "sbt-version-policy"        % "1.0.0-RC5")
addSbtPlugin("com.codecommit"            % "sbt-github-actions"        % "0.10.1")
addSbtPlugin("com.eed3si9n"              % "sbt-buildinfo"             % "0.9.0")
addSbtPlugin("com.geirsson"              % "sbt-ci-release"            % "1.5.5")
addSbtPlugin("com.github.cb372"          % "sbt-explicit-dependencies" % "0.2.16")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat"              % "0.1.16")
addSbtPlugin("net.vonbuchholtz"          % "sbt-dependency-check"      % "3.1.1")
addSbtPlugin("org.scalameta"             % "sbt-mdoc"                  % "2.2.18")
addSbtPlugin("org.scalameta"             % "sbt-scalafmt"              % "2.4.2")
addSbtPlugin("org.scalastyle"           %% "scalastyle-sbt-plugin"     % "1.0.0")
addSbtPlugin("org.wartremover"           % "sbt-wartremover"           % "2.4.13")

lazy val root = (project in file(".")).dependsOn(sonatypePlugin)

lazy val sonatypePlugin = RootProject(
  uri("git://github.com/steinybot/sbt-sonatype#5a0ace0551b5d58debbf4aae4a1b5f9b5d888ef1")
)
