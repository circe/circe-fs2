ThisBuild / organization := "io.circe"

val compilerOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen"
)

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

def priorTo2_13(scalaVersion: String): Boolean =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, minor)) if minor < 13 => true
    case _                              => false
  }

val baseSettings = Seq(
  scalacOptions ++= compilerOptions,
  scalacOptions ++= (
    if (priorTo2_13(scalaVersion.value))
      Seq(
        "-Xfuture",
        "-Yno-adapted-args",
        "-Ywarn-unused-import"
      )
    else
      Seq(
        "-Ywarn-unused:imports"
      )
  ),
  Compile / console / scalacOptions ~= {
    _.filterNot(Set("-Ywarn-unused-import"))
  },
  Test / console / scalacOptions ~= {
    _.filterNot(Set("-Ywarn-unused-import"))
  },
  coverageHighlighting := true,
  coverageEnabled := (if (scalaVersion.value.startsWith("2.13")) coverageEnabled.value else false),
  Compile / scalastyleSources ++= (Compile / unmanagedSourceDirectories).value
)

val allSettings = baseSettings ++ publishSettings
val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

val docMappingsApiDir = settingKey[String]("Subdirectory in site target directory for API docs")

lazy val root = project.in(file(".")).settings(allSettings).settings(noPublishSettings).aggregate(fs2.jvm, fs2.js)

lazy val fs2 = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("fs2"))
  .settings(allSettings)
  .jsSettings(coverageEnabled := false)
  .settings(
    moduleName := "circe-fs2",
    mimaPreviousArtifacts := Set("io.circe" %% "circe-fs2" % previousCirceFs2Version),
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
    ),
    ghpagesNoJekyll := true,
    docMappingsApiDir := "api",
    addMappingsToSiteDir(Compile / packageDoc / mappings, docMappingsApiDir)
  )

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releaseVcsSign := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/circe/circe-fs2")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots".at(nexus + "content/repositories/snapshots"))
    else
      Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
  },
  /* Someday maybe Scaladoc will actually work on package object-only projects.
  autoAPIMappings := true,
  apiURL := Some(url("https://circe.github.io/circe-fs2/api/")),
   */
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/circe/circe-fs2"),
      "scm:git:git@github.com:circe/circe-fs2.git"
    )
  ),
  developers := List(
    Developer(
      "travisbrown",
      "Travis Brown",
      "travisrobertbrown@gmail.com",
      url("https://twitter.com/travisbrown")
    )
  )
)

credentials ++= (
  for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username,
    password
  )
).toSeq

ThisBuild / githubWorkflowJavaVersions := Seq("adopt@1.8")
// No auto-publish atm. Remove this line to generate publish stage
ThisBuild / githubWorkflowPublishTargetBranches := Seq.empty
ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(
    List("clean", "coverage", "test", "coverageReport", "scalafmtCheckAll"),
    id = None,
    name = Some("Test")
  ),
  WorkflowStep.Use(
    UseRef.Public("codecov", "codecov-action", "e156083f13aff6830c92fc5faa23505779fbf649"), // v1.2.1
    name = Some("Upload code coverage")
  )
)
