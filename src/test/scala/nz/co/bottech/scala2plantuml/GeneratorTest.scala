package nz.co.bottech.scala2plantuml

import utest.assert

import java.io.{IOException, InputStream}
import scala.util.{Try, Using}

trait GeneratorTest {

  protected val exampleDir: String

  private def testSemanticdb(name: String) =
    s"META-INF/semanticdb/src/test/scala/nz/co/bottech/scala2plantuml/examples/$exampleDir/$name.scala.semanticdb"

  protected def success(name: String, diagram: String): Unit = {
    val result = generate(name).get
    assert(result == Right(diagram))
  }

  protected def generate(name: String): Try[Either[String, String]] = {
    val path = testSemanticdb(name)
    Using(getResource(path))(ClassDiagramGenerator.fromInputStream)
  }

  private def getResource(path: String): InputStream =
    Option(getClass.getClassLoader.getResourceAsStream(path)).getOrElse {
      throw new IOException(s"Resource not found: $path")
    }
}
