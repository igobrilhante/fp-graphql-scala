package com.igobrilhante.graphqlscraper.core.sangria

import scala.concurrent.ExecutionContext

import cats.effect.Async
import cats.effect.std.Dispatcher
import cats.syntax.all._
import com.igobrilhante.graphqlscraper.core.repository.{NewsPostgresRepository, NewsRepository}
import com.igobrilhante.graphqlscraper.core.schema.{GraphQL, GraphqlSchema}
import doobie.Transactor

trait SangriaComponents {

  // Construct a GraphQL implementation based on our Sangria definitions.
  def graphQL[F[_]: Async](
      dispatcher: Dispatcher[F],
      transactor: Transactor[F]
  )(implicit ec: ExecutionContext): GraphQL[F] =
    SangriaGraphQL.impl[F](GraphqlSchema.schema(dispatcher), NewsPostgresRepository.impl(transactor).pure[F])

  // Construct a GraphQL implementation based on our Sangria definitions.
  def graphQL[F[_]: Async](
      dispatcher: Dispatcher[F],
      repository: NewsRepository[F]
  )(implicit ec: ExecutionContext): GraphQL[F] =
    SangriaGraphQL.impl[F](GraphqlSchema.schema(dispatcher), repository.pure[F])

}
