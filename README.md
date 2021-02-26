# Scala2PlantUML

Scala2PlantUML is a library / CLI / sbt plugin for converting Scala code to [PlantUML] diagrams.

Scala2PlantUML consumes [SemanticDB] files.

## sbt

For sbt versions `>= 1.3.0`:

```sbt
semanticdbEnabled := true
```

For sbt versions `< 1.3.0`:

![SemanticDB Version](https://img.shields.io/maven-central/v/org.scalameta/semanticdb-scalac_2.12.13?label=SemanticDB)

```sbt
addCompilerPlugin("org.scalameta" % "semanticdb-scalac" % "x.y.z" cross CrossVersion.full)
scalacOptions += "-Yrangepos"
```

[plantuml]: https://plantuml.com/
[semanticdb]: https://scalameta.org/docs/semanticdb/guide.html
