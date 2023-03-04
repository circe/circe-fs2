ThisBuild / tlBaseVersion := "0.14"
ThisBuild / startYear := Some(2017)
ThisBuild / developers ++= List(
  Developer(
    "travisbrown",
    "Travis Brown",
    "travisrobertbrown@gmail.com",
    url("https://twitter.com/travisbrown")
  )
)

val circeVersion = "0.14.5"
val fs2Version = "3.5.0"
val jawnVersion = "1.4.0"
val previousCirceFs2Version = "0.13.0"

val munitVersion = "1.0.0-M7"
val scalacheckEffectVersion = "2.0.0-M2"
val munitEffectVersion = "2.0.0-M3"

val scala212 = "2.12.17"
val scala213 = "2.13.10"

ThisBuild / scalaVersion := scala213
ThisBuild / crossScalaVersions := Seq(scala212, scala213, "3.2.2")

ThisBuild / tlCiReleaseBranches := Seq("master")

lazy val root = tlCrossRootProject.aggregate(fs2)

lazy val fs2 = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("fs2"))
  .settings(
    name := "circe-fs2",
    libraryDependencies ++= Seq(
      "co.fs2" %%% "fs2-core" % fs2Version,
      "io.circe" %%% "circe-jawn" % circeVersion,
      "io.circe" %%% "circe-generic" % circeVersion % Test,
      "io.circe" %%% "circe-testing" % circeVersion % Test,
      "org.scalameta" %%% "munit-scalacheck" % munitVersion % Test,
      "org.typelevel" %%% "scalacheck-effect-munit" % scalacheckEffectVersion % Test,
      "org.typelevel" %%% "munit-cats-effect" % munitEffectVersion % Test,
      "org.typelevel" %%% "jawn-parser" % jawnVersion
    )
  )
  .platformsSettings(JSPlatform, NativePlatform)(
    tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "0.14.1").toMap
  )
