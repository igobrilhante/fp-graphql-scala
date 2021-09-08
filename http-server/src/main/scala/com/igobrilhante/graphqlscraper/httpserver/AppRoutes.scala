package com.igobrilhante.graphqlscraper.httpserver

import scala.concurrent.duration._

import cats.data.Kleisli
import cats.effect.kernel.Async
import cats.implicits._
import com.igobrilhante.graphqlscraper.core.entities.{ApiError, GraphQLApiQuery}
import com.igobrilhante.graphqlscraper.core.logging.Logging
import com.igobrilhante.graphqlscraper.core.schema.GraphQL
import fs2.{Pipe, Stream}
import io.circe.Json
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.middleware.Logger
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text
import org.http4s.{HttpRoutes, Request, Response}
import sangria.execution.QueryAnalysisError
import sangria.parser.SyntaxError

object AppRoutes extends Logging {

  def apply[F[_]](graphQL: GraphQL[F])(implicit F: Async[F]): Kleisli[F, Request[F], Response[F]] = {
    AppRoutes(graphQL, HttpRoutes.empty)
  }

  def apply[F[_]](graphQL: GraphQL[F], websocketRoutes: HttpRoutes[F])(implicit
      F: Async[F]
  ): Kleisli[F, Request[F], Response[F]] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    def handleGraphQLResponse(response: Either[Throwable, Json]) = {
      response match {
        case Right(json) => Ok(json)
        //
        case Left(exception: QueryAnalysisError) =>
          logger.error("Invalid query", exception)
          BadRequest(ApiError(exception))
        //
        case Left(exception: SyntaxError) =>
          logger.error(exception.getMessage(), exception)
          BadRequest(ApiError(exception))
        //
        case Left(exception) =>
          logger.error(exception.getMessage, exception)
          InternalServerError(ApiError(exception))
      }
    }

    // Provided by `cats.effect.IOApp`, needed elsewhere:
//    implicit val timer: Timer[IO] = IO.timer(global)

    // An infinite stream of the periodic elapsed time
    val seconds = Stream.awakeEvery[F](1.second)

    val routes = HttpRoutes.of[F] {

      // Example websocket endpoint
      case GET -> Root / "ws" =>
        Ok(seconds.map(_.toString))

        val toClient: Stream[F, WebSocketFrame] =
          Stream.awakeEvery[F](1.seconds).map(d => Text(s"Ping! $d"))

        val fromClient: Pipe[F, WebSocketFrame, Unit] = _.evalMap {
          case Text(t, _) => F.delay(println(t))
          case f          => F.delay(println(s"Unknown type: $f"))
        }
        WebSocketBuilder[F].build(toClient, fromClient)

      case request @ POST -> Root / "news" =>
        request
          .as[GraphQLApiQuery]
          .flatMap(graphQL.query)
          .flatMap(handleGraphQLResponse)
    }

    val service = (routes <+> websocketRoutes).orNotFound

    val routesWithLogger = Logger.httpApp[F](logHeaders = true, logBody = false)(service)

    routesWithLogger
  }

}
