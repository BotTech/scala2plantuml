name := "scala2plantuml"
scalaVersion := "2.13.4"
// TODO: Add tpolecat plugin.
scalacOptions += "-Ywarn-unused"
libraryDependencies ++= List(
  "org.scalameta" %% "scalameta" % semanticdbVersion.value,
  "org.scalameta" %% "trees" % semanticdbVersion.value,
  "org.slf4j" % "slf4j-api" % "1.7.30",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
  "ch.qos.logback" % "logback-core" % "1.2.3" % Test,
  "com.lihaoyi" %% "utest" % "0.7.7" % Test
)
testFrameworks += new TestFramework("utest.runner.Framework")
semanticdbEnabled := true
semanticdbVersion := "4.4.9"
Test / fullClasspath += (Test / semanticdbTargetRoot).value
