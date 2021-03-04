import utest._

TaskKey[Unit]("check") := {
  val sourceDirs = (Compile / sourceDirectories).value
  val base = baseDirectory.value
  val expected = sourceDirs.flatMap(_.relativeTo(base))
  val actual = (Compile / scala2PlantUML / sourceDirectories).value
  assert(actual == expected)
}
