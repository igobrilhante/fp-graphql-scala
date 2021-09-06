package com.igobrilhante.graphqlscraper.httpserver

import scala.concurrent.ExecutionContext.Implicits.global

import cats.effect.Resource
import cats.effect.kernel.Async
import com.igobrilhante.graphqlscraper.core.caliban.CalibanComponents
import com.igobrilhante.graphqlscraper.core.repository.RepositoryComponents
import org.http4s.HttpApp
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Server
import zio.Task

object AppServerWithCaliban {

  def impl()(implicit F: Async[Task]): AppServerZio = new AppServerZio
    with RepositoryComponents
    with CalibanComponents {

    override def server(routes: HttpApp[Task]): Resource[Task, Server] = {

      BlazeServerBuilder[Task](global)
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(routes)
        .resource
    }

    override def serverFromResource(): Resource[Task, Server] =
      for {
        hikariTransactor <- transactor[Task]()
        graphQLInstance = graphQL(hikariTransactor)
        rts             = AppRoutes[Task](graphQLInstance)
        server <- server(rts)
      } yield server

  }

}
