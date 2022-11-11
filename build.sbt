ThisBuild / tlBaseVersion := "0.14"
ThisBuild / circeRootOfCodeCoverage := None
ThisBuild / startYear := Some(2017)
ThisBuild / scalafixScalaBinaryVersion := "2.12"

val circeVersion = "0.14.3"
val fs2Version = "3.3.0"
val jawnVersion = "1.4.0"
val previousCirceFs2Version = "0.13.0"

val scalaTestVersion = "3.2.14"
val scalaTestPlusVersion = "3.2.14.0"
val catsEffectTestingVersion = "1.4.0"
val scalacheckEffectVersion = "2.0.0-M2"

val scala212 = "2.12.17"
val scala213 = "2.13.10"

ThisBuild / scalaVersion := scala213
ThisBuild / crossScalaVersions := Seq(scala212, scala213, "3.2.1")

lazy val root = tlCrossRootProject.aggregate(fs2)

lazy val fs2 = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("fs2"))
  .settings(
    name := "circe-fs2",
    libraryDependencies ++= Seq(
      "co.fs2" %%% "fs2-core" % fs2Version,
      "io.circe" %%% "circe-jawn" % circeVersion,
      "io.circe" %%% "circe-generic" % circeVersion % Test,
      "io.circe" %%% "circe-testing" % circeVersion % Test,
      "org.scalatest" %%% "scalatest" % scalaTestVersion % Test,
      "org.scalatestplus" %%% "scalacheck-1-16" % scalaTestPlusVersion % Test,
      "org.typelevel" %%% "cats-effect-testing-scalatest" % catsEffectTestingVersion % Test,
      "org.typelevel" %%% "scalacheck-effect" % scalacheckEffectVersion % Test,
      "org.typelevel" %%% "jawn-parser" % jawnVersion
    )
  )
  .jsSettings(
    tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "0.14.1").toMap
  )
