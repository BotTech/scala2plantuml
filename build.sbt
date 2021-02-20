name := "scala2plantuml"
scalaVersion := "2.13.4"
libraryDependencies += "org.scalameta" %% "scalameta" % "4.4.8"
libraryDependencies += "com.lihaoyi" %% "utest" % "0.7.7" % Test
testFrameworks += new TestFramework("utest.runner.Framework")
