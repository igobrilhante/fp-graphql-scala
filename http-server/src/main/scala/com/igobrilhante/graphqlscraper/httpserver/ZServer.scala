package com.igobrilhante.graphqlscraper.httpserver

import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.{Task, URIO, ZIO}

object ZServer extends zio.App {

  override def run(args: List[String]): URIO[zio.ZEnv, zio.ExitCode] = {
    AppServer
      .impl()
      .serverFromResource[Task]()
      .use { _ => ZIO.never }
      .exitCode

  }
}
