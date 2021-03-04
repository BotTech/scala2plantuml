import utest._

TaskKey[Unit]("check") := {
  val sourceDirs = (Compile / sourceDirectories).value ++ (Test / sourceDirectories).value
  val base = baseDirectory.value
  val expected = sourceDirs.flatMap(_.relativeTo(base))
  val actual = (Test / scala2PlantUML / sourceDirectories).value
  assert(actual == expected)
}
