name := "soundcloud"
version := "0.1"
scalaVersion := "2.11.8"
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.3.5"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-testkit" % akkaV,
    "com.typesafe.akka" % "akka-stream-experimental_2.11" % "2.0.5",
    "com.github.nscala-time" %% "nscala-time" % "2.16.0",
    "net.debasishg" %% "redisclient" % "3.4"
  )
}
