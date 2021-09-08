package com.igobrilhante.graphqlscraper.httpserver

import scala.concurrent.ExecutionContext.Implicits.global

import cats.effect.std.Dispatcher
import cats.effect.{Async, Resource}
import com.igobrilhante.graphqlscraper.core.repository.RepositoryComponents
import com.igobrilhante.graphqlscraper.core.sangria.SangriaComponents
import org.http4s.HttpApp
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Server
import zio.{Task, ZManaged}

trait AppServer {

  def server[F[_]: Async](routes: HttpApp[F]): Resource[F, Server]

  // Resource that constructs our final server.
  def serverFromResource[F[_]: Async](): Resource[F, Server]
}

trait AppServerZio {

  def server(routes: HttpApp[Task]): ZManaged[Any, Throwable, Server]

  // Resource that constructs our final server.
  def serverFromResource(): ZManaged[Any, Throwable, Server]
}

object AppServer {

  def impl(): AppServer = new AppServer with RepositoryComponents with SangriaComponents {

    def server[F[_]: Async](routes: HttpApp[F]): Resource[F, Server] =
      BlazeServerBuilder[F](global)
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(routes)
        .resource

    // Resource that constructs our final server.
    def serverFromResource[F[_]: Async](): Resource[F, Server] =
      for {
        dispatcher       <- Dispatcher[F].map(identity)
        hikariTransactor <- transactor[F]()
        graphQLInstance = graphQL[F](dispatcher, hikariTransactor)
        rts             = AppRoutes(graphQLInstance)
        server <- server[F](rts)
      } yield server
  }

}
