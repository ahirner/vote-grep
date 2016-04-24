scalaVersion := "2.11.8"

lazy val common = Seq(
  organization := "org.votegrep",
  version := "0.0-PoC",
  scalaVersion := "2.11.8"
)
/*
lazy val vote-grep = (project in file(".")).
  settings(common: _*)
*/

lazy val server = (project in file("server")).
  settings(common: _*).
  settings(
    name := "server"
  )

lazy val client = (project in file("client")).
  settings(common: _*).
  settings(
    name := "client"
  )

lazy val http4sVersion = "0.13.1"
lazy val akkaVersion = "2.4.3"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
  "com.github.melrief" %% "purecsv" % "0.0.6",

  "org.jsoup" % "jsoup" % "1.8.3",
  "com.joestelmach" % "natty" % "0.12",
  "net.sourceforge.htmlunit" % "htmlunit" % "2.21",
  "rome" % "rome" % "1.0",
  "org.seleniumhq.selenium" % "selenium-java" % "2.53.0"
)

// use ammonite
libraryDependencies += "com.lihaoyi" % "ammonite-repl" % "0.5.7" % "test" cross CrossVersion.full
initialCommands in (Test, console) := """ammonite.repl.Main.run("import org.votegrep._; import java.net.URL")"""

resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
