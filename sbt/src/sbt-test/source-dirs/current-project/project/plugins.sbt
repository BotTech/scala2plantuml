sys.props.get("plugin.version") match {
  case Some(version) => addSbtPlugin("nz.co.bottech" % "sbt-scala2plantuml" % version)
  case _             => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}

libraryDependencies += "com.lihaoyi" %% "utest" % "0.7.11"
