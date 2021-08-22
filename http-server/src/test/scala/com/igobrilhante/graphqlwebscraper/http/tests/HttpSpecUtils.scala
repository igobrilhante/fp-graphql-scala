package com.igobrilhante.graphqlwebscraper.http.tests

import cats.data.Kleisli
import cats.effect._
import cats.effect.std.Dispatcher
import com.igobrilhante.graphqlscraper.core.repository.NewsRepository
import com.igobrilhante.graphqlscraper.core.tests.SpecUtils
import com.igobrilhante.graphqlscraper.httpserver.AppRoutes
import org.http4s._
import org.http4s.implicits.http4sLiteralsSyntax
import org.typelevel.ci.CIString

trait HttpSpecUtils extends SpecUtils {

  def routesResource[F[_]: Async](repository: NewsRepository[F]): Resource[F, Kleisli[F, Request[F], Response[F]]] =
    for {
      dispatcher <- Dispatcher[F].map(identity)
      graphQLInstance = graphQL(dispatcher, repository)
      routes          = AppRoutes(graphQLInstance)
    } yield routes

  def makeRequest[F[_]: Async](body: EntityBody[F]): Request[F]#Self#Self =
    Request[F](method = Method.POST, uri = uri"/news")
      .withBodyStream(body)
      .withHeaders(
        Header.Raw(CIString("Content-Type"), "application/json")
      )

}
