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
  "dev.zio" %% "zio-nio" % "2.0.2",
  "dev.zio" %% "zio-test" % "2.1.5" % Test,
  "io.getquill" %% "quill-zio" % "4.7.0",
  "io.getquill" %% "quill-jdbc-zio" % "4.8.5" exclude ("com.lihaoyi", "geny_2.13"),
  "org.postgresql" % "postgresql" % "42.6.0",
  "com.github.jwt-scala" %% "jwt-zio-json" % "10.0.1",
  "com.github.t3hnar" %% "scala-bcrypt" % "4.3.0" cross CrossVersion.for3Use2_13,
  "org.scalatest" %% "scalatest" % "3.2.11" % Test,
  "org.flywaydb"  % "flyway-core" % "9.21.1",
  "com.lihaoyi" %% "ujson" % "3.1.2",
)
