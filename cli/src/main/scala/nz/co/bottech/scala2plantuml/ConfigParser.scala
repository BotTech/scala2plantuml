package nz.co.bottech.scala2plantuml

import scopt._

import java.io.File

object ConfigParser {

  final private object Terminated extends RuntimeException

  private val parserSetup: OParserSetup = new DefaultOParserSetup {
    override def showUsageOnError: Option[Boolean] = Some(true)
  }

  private val effectSetup: OEffectSetup = new DefaultOEffectSetup {

    // Don't exit otherwise sbt traps this and there are confusing errors.
    // This is especially troublesome when running this from mdoc.
    @SuppressWarnings(Array("org.wartremover.warts.Throw"))
    override def terminate(exitState: Either[String, Unit]): Unit =
      throw Terminated
  }

  // scalastyle:off method.length
  private val parser: OParser[Unit, Config] = {
    val builder = OParser.builder[Config]
    import builder._
    OParser.sequence(
      programName("scala2plantuml"),
      head(s"Scala2PlantUML version ${BuildInfo.version}"),
      note(
        """Scala2PlantUML generates PlantUML Class Diagrams from Scala SemanticDB files.
          |""".stripMargin
      ),
      arg[String]("symbol")
        .action((symbol, config) => config.copy(symbol = symbol))
        .text(
          """The symbol to use as the starting point for generating the diagram.
            |
            |To get a symbol from a class name, convert the package name separate '.' to '/' and add a '#'
            |suffix. For an object use a suffix of '.'.
            |
            |See https://scalameta.org/docs/semanticdb/specification.html#symbol-1 for the full syntax.
            |
            |Examples:
            |  'com/example/Foo#' (class com.example.Foo)
            |  'com/example/Foo.' (object com.example.Foo)
            |  'com/example/Foo.bar.' (value/variable bar on object com.example.Foo)
            |  'com/example/Foo#baz().' (function baz on class com.example.Foo)""".stripMargin
        ),
      note(
        """
          |The --include and --exclude options control which symbols will be processed. Each of these can be provided multiple times.
          |
          |The pattern supports two wildcards:
          |1) ** (matches any character)
          |2) *  (matches all characters except for '/')
          |""".stripMargin
      ),
      opt[String]('i', "include")
        .valueName("<pattern>")
        .unbounded()
        .action((pattern, config) => config.addInclude(pattern))
        .text(
          """Only include symbols that match the pattern.
            |
            |Default: '**'
            |
            |Example:
            |  --include 'com/example/**/model/*'""".stripMargin
        ),
      note(""),
      opt[String]('e', "exclude")
        .valueName("<pattern>")
        .unbounded()
        .action((pattern, config) => config.addExclude(pattern))
        .text(
          """Excludes all symbols that match the pattern.
            |
            |Default: 'scala/**', 'java/**'
            |
            |Example:
            |  --exclude 'com/example/**/data/*'""".stripMargin
        ),
      note(
        """
          |The --dir, --jar, and --url options specify the directories and JAR files that are used when locating SemanticDB files.
          |Each of these can be provided multiple times.
          |
          |By default, the classpath that was used when executing Scala2PlantUML is also used.
          |""".stripMargin
      ),
      opt[File]('d', "dir")
        .valueName("<dir>")
        .unbounded()
        .action((dir, config) => config.addDirectory(dir))
        .text(
          """Directories of the SemanticDB target roots containing META-INF/semanticdb/**/*.semanticdb files.
            |
            |Example:
            |  --dir 'my-project/target/scala-2.13/meta'""".stripMargin
        ),
      note(""),
      opt[File]('j', "jar")
        .valueName("<jar>")
        .unbounded()
        .action((jar, config) => config.addDirectory(jar))
        .text(
          """JAR containing META-INF/semanticdb/**/*.semanticdb files.
            |
            |Example:
            |  --jar 'foo.jar'
            |""".stripMargin
        ),
      note(""),
      opt[File]('u', "url")
        .valueName("<url>")
        .unbounded()
        .action((url, config) => config.addDirectory(url))
        .text(
          """A URL to a JAR containing META-INF/semanticdb/**/*.semanticdb files.
            |
            |Example:
            |  --url 'https://repo1.maven.org/maven2/com/example/foo/foo_2.13/1.0.0/foo_2.13-1.0.0-semanticdb.jar'""".stripMargin
        ),
      note(
        """
          |The --project and --source options specify where within the search locations the SemanticDB files can be found.
          |Each of these can be provided multiple times. The result will be all combinations of projects and source roots.
          |""".stripMargin
      ),
      opt[String]('p', "project")
        .valueName("<project>")
        .unbounded()
        .action((project, config) => config.addProject(project))
        .text(
          """The name of the projects that have SemanticDB files.
            |
            |The project name will be used when looking for SemanticDB files such as:
            |META-INF/semanticdb/<project>/<source>/*.semanticdb
            |
            |An empty project name will search in:
            |META-INF/semanticdb/<source>/*.semanticdb
            |
            |Default: ''
            |
            |Example:
            |  --project my-project""".stripMargin
        ),
      note(""),
      opt[String]('s', "source")
        .valueName("<source>")
        .unbounded()
        .action((root, config) => config.addSourceRoot(root))
        .text(
          """The directory relative to the project where the source files were located.
            |
            |The source will be used when looking for SemanticDB files such as
            |META-INF/semanticdb/<project>/<source>/*.semanticdb.
            |
            |Default: src/main/scala
            |
            |Example:
            |  --source 'source/scala'""".stripMargin
        ),
      note(""),
      opt[Int]('l', "max-level")
        .valueName("<level>")
        .optional()
        .validate(level => Either.cond(level > 0, (), "Level must be positive."))
        .action((level, config) => config.copy(maxLevel = Some(level)))
        .text(
          """The maximum number of levels that will be traversed when following symbol references.
            |
            |This means that parent symbols that would be beyond the max level will not be shown.
            |
            |A diagram with a max-level of 1 will only contain the initial symbol.
            |
            |Default: Unlimited
            |
            |Example:
            |  --max-level 3""".stripMargin
        ),
      note(""),
      opt[File]('o', "output")
        .valueName("<file>")
        .action((file, config) => config.copy(outputFile = Some(file)))
        .text(
          """Write the output to the given file.
            |
            |Example:
            |  --output docs/diagrams/my-project.puml""".stripMargin
        ),
      note(""),
      opt[Boolean]('c', "colour")
        .action((colour, config) => config.copy(logInColour = colour))
        .text(
          """Enables coloured output.
            |
            |Default: true
            |
            |Example:
            |  --colour false""".stripMargin
        ),
      note(""),
      opt[Unit]('v', "verbose")
        .unbounded()
        .action((_, config) => config.increaseLogLevel)
        .text(
          """Increases the log level.
            |
            |This can be provided twice for the most verbose logging.
            |
            |Example:
            |  -vv""".stripMargin
        ),
      note(""),
      help('h', "help"),
      version("version"),
      checkConfig(validateConfig)
    )
  }
  // scalastyle:on method.length

  def parse(args: Array[String]): Option[Config] =
    try OParser.parse(parser, args, Config(), parserSetup, effectSetup)
    catch {
      case Terminated => None
    }

  private def validateConfig(config: Config): Either[String, Unit] =
    if (config.ignore(config.symbol)) Left("Symbol must match include patterns and not match exclude patterns.")
    else Right(())
}
