name := "soundcloud"
version := "1.0"
organization := "com.gmail.@.qtomerr"

scalaVersion := "2.11.8"
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaVersion = "2.3.5"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
    "com.typesafe.akka" % "akka-stream-experimental_2.11" % "2.0.5"
  )
}
