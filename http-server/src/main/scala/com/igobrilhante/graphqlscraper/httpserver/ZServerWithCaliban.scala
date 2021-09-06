package com.igobrilhante.graphqlscraper.httpserver

import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.{URIO, ZIO}

object ZServerWithCaliban extends zio.App {
  override def run(args: List[String]): URIO[zio.ZEnv, zio.ExitCode] = {
    AppServerWithCaliban
      .impl()
      .serverFromResource()
      .toManagedZIO
      .use { _ => ZIO.never }
      .exitCode

  }
}
