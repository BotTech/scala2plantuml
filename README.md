# Scala2PlantUML

Scala2PlantUML generates [PlantUML] diagrams from Scala code.

It comes as a standalone library, a CLI tool and an sbt plugin.

Scala2PlantUML consumes [SemanticDB] files so you will need to know how to create those or simply follow the sbt setup
instructions below.

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
addSbtPlugin("nz.co.bottech" % "sbt-scala2plantuml" % "0.1.0")
```

## CLI

### Install

Use [Coursier] to create a launcher for Scala2PlantUML:

```shell
cs install --channel https://git.io/Jqv1i scala2plantuml
```

### Usage

```text
Scala2PlantUML version 0.1.0
Usage: scala2plantuml [options] symbol

Scala2PlantUML generates PlantUML Class Diagrams from Scala SemanticDB files.

  symbol                   The symbol to use as the starting point for generating the diagram.

                           To get a symbol from a class name, convert the package name separate '.' to '/' and add a '#'
                           suffix. For an object use a suffix of '.'.

                           See https://scalameta.org/docs/semanticdb/specification.html#symbol-1 for the full syntax.

                           Examples:
                             'com/example/Foo#' (class com.example.Foo)
                             'com/example/Foo.' (object com.example.Foo)
                             'com/example/Foo.bar.' (value/variable bar on object com.example.Foo)
                             'com/example/Foo#baz().' (function baz on class com.example.Foo)

The --include and --exclude options control which symbols will be processed. Each of these can be provided multiple times.

The pattern supports two wildcards:
1) ** (matches any character)
2) *  (matches all characters except for '/')

  -i, --include <pattern>  Only include symbols that match the pattern.

                           Default: '**'

                           Example:
                             --include 'com/example/**/model/*'

  -e, --exclude <pattern>  Excludes all symbols that match the pattern.

                           Default: 'scala/**', 'java/**'

                           Example:
                             --exclude 'com/example/**/data/*'

The --dir, --jar, and --url options specify the directories and JAR files that are used when locating SemanticDB files.
Each of these can be provided multiple times.

By default, the classpath that was used when executing Scala2PlantUML is also used.

  -d, --dir <dir>          Directories of the SemanticDB target roots containing META-INF/semanticdb/**/*.semanticdb files.

                           Example:
                             --dir 'my-project/target/scala-2.13/meta'

  -j, --jar <jar>          JAR containing META-INF/semanticdb/**/*.semanticdb files.

                           Example:
                             --jar 'foo.jar'

  -u, --url <url>          A URL to a JAR containing META-INF/semanticdb/**/*.semanticdb files.

                           Example:
                             --url 'https://repo1.maven.org/maven2/com/example/foo/foo_2.13/1.0.0/foo_2.13-1.0.0-semanticdb.jar'

The --project and --source options specify where within the search locations the SemanticDB files can be found.
Each of these can be provided multiple times. The result will be all combinations of projects and source roots.

  -p, --project <project>  The name of the projects that have SemanticDB files.

                           The project name will be used when looking for SemanticDB files such as:
                           META-INF/semanticdb/<project>/<source>/*.semanticdb

                           An empty project name will search in:
                           META-INF/semanticdb/<source>/*.semanticdb

                           Default: ''

                           Example:
                             --project my-project

  -s, --source <source>    The directory relative to the project where the source files were located.

                           The source will be used when looking for SemanticDB files such as
                           META-INF/semanticdb/<project>/<source>/*.semanticdb.

                           Default: src/main/scala

                           Example:
                             --source 'source/scala'

  -o, --output <file>      Write the output to the given file.

                           Example:
                             --output docs/diagrams/my-project.puml

  -c, --colour <value>     Enables coloured output.

                           Default: true

                           Example:
                             --colour false

  -v, --verbose            Increases the log level.

                           This can be provided twice for the most verbose logging.

                           Example:
                             -vv

  -h, --help
  --version
```

## Library

> 🚧 TODO: Document Library.

[coursier]: https://get-coursier.io/docs/cli-install
[plantuml]: https://plantuml.com/
[semanticdb]: https://scalameta.org/docs/semanticdb/guide.html
