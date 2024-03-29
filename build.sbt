name := """event-sourcing-app"""

val commonSettings = Seq(
  organization := "com.github.stzr1123",
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.13.4"
)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .enablePlugins(PlayScala)
  .aggregate(events)
  .dependsOn(events)

lazy val events = (project in file("events")).settings(commonSettings)
  .settings(Seq(libraryDependencies := Seq(
    "com.typesafe.play" %% "play-json" % "2.8.1")))

pipelineStages := Seq(digest)

libraryDependencies ++= Seq(
  jdbc,
  evolutions,
  "com.softwaremill.macwire" %% "macros" % "2.3.3" % "provided",
  "org.postgresql" % "postgresql" % "42.2.18",
  "org.scalikejdbc" %% "scalikejdbc" % "3.5.0",
  "org.scalikejdbc" %% "scalikejdbc-config"  % "3.5.0",
  "ch.qos.logback"  %  "logback-classic" % "1.2.3",
  "de.svenkubiak" % "jBCrypt" % "0.4.1",
  "com.typesafe.akka" %% "akka-stream-kafka" % "2.0.5",
  "io.reactivex.rxjava3" % "rxjava" % "3.0.8",
  "org.neo4j.driver" % "neo4j-java-driver" % "4.2.0"
)
