package com.igobrilhante.graphqlscraper.httpserver

import cats.effect._

object ServerIO extends IOApp {

  // Our entry point starts the server and blocks forever.
  def run(args: List[String]): IO[ExitCode] = {

    AppServer
      .impl()
      .serverFromResource[IO]()
      .use(_ => IO.never.as(ExitCode.Success))
  }

}
