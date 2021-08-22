import sbt._

object Dependencies {

  lazy val http4sVersion        = "0.23.1"
  lazy val doobieVersion        = "1.0.0-M5"
  lazy val logbackVersion       = "1.3.0-alpha9"
  lazy val catsEffectVersion    = "3.2.2"
  lazy val catsVersion          = "2.6.1"
  lazy val circleVersion        = "0.14.1"
  lazy val circleConfigVersion  = "0.8.0"
  lazy val sangriaVersion       = "2.1.3"
  lazy val sangriaCircleVersion = "1.3.2"
  lazy val scalaScraperVersion  = "2.2.1"
  //
  lazy val scalaTestVersion      = "3.2.9"
  lazy val zioVersion            = "1.0.10"
  lazy val zioInteropCatsVersion = "3.1.1.0"

  lazy val sangria       = "org.sangria-graphql" %% "sangria"       % sangriaVersion
  lazy val sangriaCircle = "org.sangria-graphql" %% "sangria-circe" % sangriaCircleVersion

  lazy val doobieQuill    = "org.tpolecat" %% "doobie-quill"    % doobieVersion
  lazy val doobieCore     = "org.tpolecat" %% "doobie-core"     % doobieVersion
  lazy val doobieHikari   = "org.tpolecat" %% "doobie-hikari"   % doobieVersion
  lazy val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % doobieVersion

  lazy val postgres           = "org.postgresql" % "postgresql"           % "42.2.23"
  lazy val quillAsyncPostgres = "io.getquill"   %% "quill-async-postgres" % "3.9.0"

  lazy val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion
  lazy val catsCore   = "org.typelevel" %% "cats-core"   % catsVersion

  lazy val circleCore    = "io.circe" %% "circe-core"    % circleVersion
  lazy val circleGeneric = "io.circe" %% "circe-generic" % circleVersion
  lazy val circleParser  = "io.circe" %% "circe-parser"  % circleVersion
  lazy val circleConfig  = "io.circe" %% "circe-config"  % circleConfigVersion

  lazy val http4sBlazeClient = "org.http4s" %% "http4s-blaze-client" % http4sVersion
  lazy val http4sBlazeServer = "org.http4s" %% "http4s-blaze-server" % http4sVersion
  lazy val http4sCircle      = "org.http4s" %% "http4s-circe"        % http4sVersion
  lazy val http4sDsl         = "org.http4s" %% "http4s-dsl"          % http4sVersion

  lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackVersion
  lazy val logbackCore    = "ch.qos.logback" % "logback-core"    % logbackVersion
  lazy val slf4jApi       = "org.slf4j"      % "slf4j-api"       % "1.7.32"
  lazy val log4CatsSlf4j  = "org.typelevel" %% "log4cats-slf4j"  % "2.1.1"

  lazy val zio            = "dev.zio" %% "zio"              % zioVersion
  lazy val zioInteropCats = "dev.zio" %% "zio-interop-cats" % zioInteropCatsVersion

  lazy val scalaScraper = "net.ruippeixotog" %% "scala-scraper" % scalaScraperVersion

  // Tests
  lazy val doobieScalaTest      = "org.tpolecat"  %% "doobie-scalatest"              % doobieVersion    % Test
  lazy val scalaTestIO          = "org.typelevel" %% "cats-effect-testing-scalatest" % "1.2.0"          % Test
  lazy val scalaTest            = "org.scalatest" %% "scalatest"                     % scalaTestVersion % Test
  lazy val mockitoScalaCatsTest = "org.mockito"   %% "mockito-scala-cats"            % "1.16.37"        % Test
}
