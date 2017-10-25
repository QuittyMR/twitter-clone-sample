
name := "follower-maze"
version := "1.0"

scalaVersion := "2.11.8"
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
	val akkaVersion = "2.3.5"
	Seq(
		"com.typesafe.akka" %% "akka-actor" % akkaVersion,
		"com.typesafe.akka" %% "akka-testkit" % akkaVersion,
		"org.scalatest" %% "scalatest" % "3.0.4" % "test"
	)
}

// SBT-assembly plugin options
mainClass in assembly := Some("com.scfollowermaze.GlobalApp")
assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true)