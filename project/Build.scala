import sbt._
import Keys._

object Dependencies {
  val shapeless = "com.chuusai" %% "shapeless" % "2.2.5"
  val scala_reflect = "org.scala-lang" % "scala-reflect"
}

object BuildSettings {
  import Dependencies._

  val buildSettings = Defaults.defaultSettings ++ Seq(
    version := "0.1",
    scalaVersion := "2.11.7",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    scalacOptions ++= Seq(
      "-feature",
      "-unchecked",
      "-deprecation"
    ),
    libraryDependencies += shapeless
  )
}

object FromRecords extends Build {
  import BuildSettings._
  import Dependencies._

  lazy val root: Project = Project(
    "root",
    file("."),
    settings = buildSettings ++ Seq(
      run <<= run in Compile in testing)
  ) aggregate(macros, testing)

  lazy val macros: Project = Project(
    "macros",
    file("macros"),
    settings = buildSettings ++ Seq(
      libraryDependencies += scala_reflect % scalaVersion.value % "provided"
    )
  )

  lazy val testing: Project = Project(
    "testing",
    file("testing"),
    settings = buildSettings
  ) dependsOn(macros)
}
