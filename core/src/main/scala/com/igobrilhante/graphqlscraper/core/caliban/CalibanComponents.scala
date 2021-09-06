package com.igobrilhante.graphqlscraper.core.caliban

import scala.concurrent.ExecutionContext

import com.igobrilhante.graphqlscraper.core.entities.News
import com.igobrilhante.graphqlscraper.core.repository.{NewsPostgresRepository, NewsRepository, NewsSubscription}
import com.igobrilhante.graphqlscraper.core.schema.GraphQL
import doobie.Transactor
import zio.Task
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.stream.ZStream

trait CalibanComponents {
  
  def graphQL(
      transactor: Transactor[Task],
      stream: NewsSubscription[ZStream, Any, Nothing, News]
  )(implicit ec: ExecutionContext): GraphQL[Task] =
    graphQL(NewsPostgresRepository.impl(transactor), stream)

  def graphQL(
      repository: NewsRepository[Task],
      stream: NewsSubscription[ZStream, Any, Nothing, News]
  )(implicit ec: ExecutionContext): GraphQL[Task] =
    CalibanGraphQL.impl(repository, stream)

}
