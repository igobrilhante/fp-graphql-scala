package com.igobrilhante.graphqlscraper.httpserver

import scala.concurrent.ExecutionContext.Implicits.global

import caliban.{CalibanError, Http4sAdapter, RootResolver}
import cats.data.Kleisli
import cats.effect.kernel.Async
import com.igobrilhante.graphqlscraper.core.caliban.{CalibanComponents, CalibanGraphQLSchema}
import com.igobrilhante.graphqlscraper.core.entities.News
import com.igobrilhante.graphqlscraper.core.repository._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.server.{Router, Server}
import org.http4s.{HttpApp, HttpRoutes, StaticFile}
import zio.duration.durationInt
import zio.interop.catz._
import zio.stream.ZStream
import zio.{Task, _}

object AppServerWithCaliban {

  def impl()(implicit F: Async[Task]): AppServerZio = new AppServerZio
    with RepositoryComponents
    with CalibanComponents {

    override def server(routes: HttpApp[Task]): ZManaged[Any, Throwable, Server] = {

      BlazeServerBuilder[Task](global)
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(routes)
        .resource
        .toManagedZIO
    }

    override def serverFromResource(): ZManaged[Any, Throwable, Server] =
      for {
        hikariTransactor <- transactor[Task]().toManagedZIO
        repository      = NewsPostgresRepository.impl(hikariTransactor)
        stream          = new NewsSubscriptionImpl()
        graphQLInstance = graphQL(hikariTransactor, stream)
        wsRoutes <- ZManaged.fromEffect(websocketRoutes(repository, stream))
        rts = AppRoutes[Task](graphQLInstance, wsRoutes)
        server <- server(rts)
      } yield server

    private def websocketRoutes(
        repository: NewsRepository[Task],
        stream: NewsSubscription[ZStream, Any, Nothing, News]
    ): ZIO[Any, CalibanError.ValidationError, HttpRoutes[Task]] = {
      val api = caliban.GraphQL.graphQL(
        RootResolver(
          CalibanGraphQLSchema.queries(repository),
          CalibanGraphQLSchema.mutations(repository),
          CalibanGraphQLSchema.subscriptions(stream)
        )
      )

      for {
        interpreter <- api.interpreter
        router = Router[Task](
          "/live/news"  ->  CORS(Http4sAdapter.makeWebSocketService(interpreter, keepAliveTime = Some(1.second)))
        )
      } yield router
    }
  }

}
