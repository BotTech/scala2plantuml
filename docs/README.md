# Scala2PlantUML

[![Build badge](https://img.shields.io/github/workflow/status/BotTech/scala2plantuml/Continuous%20Integration/main)](https://github.com/BotTech/scala2plantuml/actions/workflows/ci.yml)
[![Maven-Central badge](https://img.shields.io/maven-central/v/nz.co.bottech/scala2plantuml_2.13)](https://search.maven.org/search?q=g:nz.co.bottech%20a:*scala2plantuml*)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

Scala2PlantUML generates [PlantUML] diagrams from Scala code.

It comes as a standalone library, a CLI tool and an sbt plugin.

Scala2PlantUML consumes [SemanticDB] files so you will need to know how to create those or simply follow the sbt setup
instructions below.

## Example

```shell
scala2plantuml \
  --url 'https://repo1.maven.org/maven2/nz/co/bottech/scala2plantuml-example_@SCALA_VERSION@/@VERSION@/scala2plantuml-example_@SCALA_VERSION@-@VERSION@.jar'\
  --project example \
  "nz/co/bottech/scala2plantuml/example/Main."
```

```scala mdoc:passthrough
println("```text")
nz.co.bottech.scala2plantuml.Scala2PlantUML.main(Array("--project", "example", "nz/co/bottech/scala2plantuml/example/Main."))
println("```")
```

![Example Class Diagram](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/BotTech/scala2plantuml/main/example/example.md)

## sbt

### Enable SemanticDB

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

### Add Scala2PlantUMLPlugin

For most use cases you should add Scala2PlantUML as a global plugin since your build is unlikely to depend on it.

Create `~/.sbt/1.0/plugins/scala2PlantUML.sbt` containing:

```text
addSbtPlugin("nz.co.bottech" % "sbt-scala2plantuml" % "@VERSION@")
```

### Generate the Diagram

Run the `scala2PlantUML` task from sbt:

```sbt
scala2PlantUML "com/example/Foo#"
```

This accepts the following arguments:
- `--include`
- `--exclude`
- `--output`

Refer to the [CLI Usage](#usage) for the definition of these arguments.

## CLI

### Install

Use [Coursier] to create a launcher for Scala2PlantUML:

```shell
cs install --channel https://git.io/Jqv1i scala2plantuml
```

### Usage

```shell
scala2plantuml --help
```

```scala mdoc:passthrough
println("```text")
nz.co.bottech.scala2plantuml.Scala2PlantUML.main(Array("--help"))
println("```")
```

## Library

> 🚧 TODO: Document Library.

## Limitations

- Only class diagrams are supported.
- Only inheritance or aggregations are supported, compositions are shown as aggregations.
- Aggregations are shown between types not between fields. There is a [bug][namespaced field links] in PlantUML which
  prevents us from being able to do this reliably.
- There is no reliable way to determine the path to a SemanticDB file from any symbol.
  If Scala2PlantUML is unable to find your symbols then the following may help:
  - Only have a single top level type in each file.
  - Ensure that the file name matches the type name.
  - Nest any subclasses of a sealed class within the companion object of the sealed class.

[coursier]: https://get-coursier.io/docs/cli-install
[plantuml]: https://plantuml.com/
[semanticdb]: https://scalameta.org/docs/semanticdb/guide.html
