name := "scala2plantuml"
scalaVersion := "2.12.13"
libraryDependencies ++= List(
  // TODO: Where is version 4.4.9 for Scala 2.13?
//  "org.scalameta" %% "semanticdb" % "4.1.6",
  "org.slf4j" % "slf4j-api" % "1.7.30",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
  "ch.qos.logback" % "logback-core" % "1.2.3" % Test,
  "com.lihaoyi" %% "utest" % "0.7.7" % Test,
//  "org.scala-lang" % "scala-compiler" % scalaVersion.value % Test,
//  "org.scalameta" % "semanticdb-scalac" % "4.4.9" % Test cross CrossVersion.full
)
testFrameworks += new TestFramework("utest.runner.Framework")
addCompilerPlugin(scalafixSemanticdb)
semanticdbEnabled := true
semanticdbVersion := scalafixSemanticdb.revision
