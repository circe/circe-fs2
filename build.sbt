ThisBuild / tlBaseVersion := "0.14"
ThisBuild / circeRootOfCodeCoverage := Some("rootJVM")

val circeVersion = "0.15.0-M1"
val fs2Version = "3.2.5"
val jawnVersion = "1.3.2"
val previousCirceFs2Version = "0.13.0"

val scalaTestVersion = "3.2.11"
val scalaTestPlusVersion = "3.2.11.0"
val catsEffectTestingVersion = "1.4.0"
val scalacheckEffectVersion = "1.0.3"

val scala212 = "2.12.15"
val scala213 = "2.13.8"

ThisBuild / crossScalaVersions := Seq(scala212, scala213, "3.1.0")

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
      "org.scalatestplus" %%% "scalacheck-1-15" % scalaTestPlusVersion % Test,
      "org.typelevel" %%% "cats-effect-testing-scalatest" % catsEffectTestingVersion % Test,
      "org.typelevel" %%% "scalacheck-effect" % scalacheckEffectVersion % Test,
      "org.typelevel" %%% "jawn-parser" % jawnVersion
    )
  )
