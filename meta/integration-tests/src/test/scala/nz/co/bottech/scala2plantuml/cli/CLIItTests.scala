package nz.co.bottech.scala2plantuml.cli

import utest._

import scala.sys.process._

object CLIItTests extends TestSuite {

  private val classpath = Option(System.getProperty("it.classpath")).get

  val tests: Tests = Tests {
    test("show help") {
      val exitCode = List("java", "-classpath", classpath, "nz.co.bottech.scala2plantuml.Scala2PlantUML").!
      assert(exitCode == 0)
    }
  }
}
