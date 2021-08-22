package com.igobrilhante.graphqlscraper.httpserver

import cats.data.Kleisli
import cats.effect.kernel.Async
import cats.implicits._
import com.igobrilhante.graphqlscraper.core.entities.{ApiError, GraphQLApiQuery}
import com.igobrilhante.graphqlscraper.core.logging.Logging
import com.igobrilhante.graphqlscraper.core.schema.GraphQL
import io.circe.Json
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.middleware.Logger
import org.http4s.{HttpRoutes, Request, Response}
import sangria.execution.QueryAnalysisError
import sangria.parser.SyntaxError

object AppRoutes extends Logging {

  def apply[F[_]](graphQL: GraphQL[F])(implicit F: Async[F]): Kleisli[F, Request[F], Response[F]] = {
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

    val service = HttpRoutes
      .of[F] { case request @ POST -> Root / "news" =>
        request
          .as[GraphQLApiQuery]
          .flatMap(graphQL.query)
          .flatMap(handleGraphQLResponse)
      }
      .orNotFound

    val routesWithLogger = Logger.httpApp[F](logHeaders = true, logBody = false)(service)

    routesWithLogger
  }

}
