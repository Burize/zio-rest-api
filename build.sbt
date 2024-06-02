ThisBuild / version := "0.1.0"
ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file("."))
  .settings(
    name := "ZIO REST API"
  )

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "2.0.22",
  "dev.zio" %% "zio-json" % "0.6.2",
  "dev.zio" %% "zio-http" % "3.0.0-RC8",
  "io.getquill" %% "quill-zio" % "4.7.0",
  "io.getquill" %% "quill-jdbc-zio" % "4.7.0",
  "org.postgresql" % "postgresql" % "42.6.0",
  "org.scalatest" %% "scalatest" % "3.2.11" % Test,
)
