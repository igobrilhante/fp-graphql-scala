import Dependencies._
import DockerSettings.skipPublishSetting

ThisBuild / scalaVersion := "2.13.6"
ThisBuild / version := "0.2.0"
ThisBuild / organization := "com.igobrilhante"
ThisBuild / organizationName := "com.igobrilhante"

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val commonsSettings = Seq(
  coverageExcludedPackages := ".*ServerIO;.*WebScraperIOApp;.*WebScraperZioApp;.*WebScraperApp;.*AppServer;.*ZServer;.*RepositoryComponents",
  coverageFailOnMinimum := false,
  coverageMinimumStmtTotal := 90,
  coverageMinimumBranchTotal := 90,
  coverageMinimumStmtPerFile := 85,
  coverageMinimumBranchPerFile := 80,
  coverageMinimumStmtPerPackage := 90,
  coverageMinimumBranchPerPackage := 85,
  libraryDependencies ++= Seq(
    scalaScraper,
    // circle
    circleCore,
    circleGeneric,
    circleParser,
    circleConfig,
    // cats
    catsCore,
    catsEffect,
    // http4s
    http4sBlazeClient,
    http4sBlazeServer,
    http4sDsl,
    http4sCircle,
    // sangria
    sangria,
    sangriaCircle,
    postgres,
    // doobie
    doobieQuill,
    doobieCore,
    doobieHikari,
    doobiePostgres,
    // zio
    zio,
    zioInteropCats,
    // logging
    logbackCore,
    logbackClassic,
    slf4jApi,
    log4CatsSlf4j,
    // Tests
    scalaTestIO,
    doobieScalaTest,
    scalaTest,
    mockitoScalaCatsTest
  ),
  // kind project plugin
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.0" cross CrossVersion.full)
)

lazy val `fp-graphql-scala` = (project in file("."))
  .settings(commonsSettings: _*)
  .settings(skipPublishSetting: _*)
  .aggregate(core, httpServer, webScraper, integrationTests)

lazy val core = (project in file("core"))
  .settings(commonsSettings: _*)
  .settings(skipPublishSetting: _*)

lazy val httpServer = (project in file("http-server"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    dockerEntrypoint := Seq("/opt/docker/bin/z-server")
  )
  .settings(DockerSettings.settings: _*)
  .settings(commonsSettings: _*)
  .dependsOn(core % "test->test;compile->compile")

lazy val webScraper = (project in file("web-scraper"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    dockerEntrypoint := Seq("/opt/docker/bin/web-scraper-zio-app")
  )
  .settings(DockerSettings.settings: _*)
  .settings(commonsSettings: _*)
  .dependsOn(core % "test->test;compile->compile")

lazy val integrationTests = (project in file("integration-tests"))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .settings(commonsSettings: _*)
  .settings(skipPublishSetting: _*)
  .dependsOn(
    core       % "it->test;test->test;compile->compile",
    httpServer % "it->test;test->test;compile->compile",
    webScraper % "it->test;test->test;compile->compile"
  )
