package com.igobrilhante.graphqlscraper.core.caliban

import scala.concurrent.ExecutionContext

import com.igobrilhante.graphqlscraper.core.repository.{NewsPostgresRepository, NewsRepository}
import com.igobrilhante.graphqlscraper.core.schema.GraphQL
import doobie.Transactor
import zio.Task
import zio.interop.catz._
import zio.interop.catz.implicits._

trait CalibanComponents {
  
  def graphQL(
      transactor: Transactor[Task]
  )(implicit ec: ExecutionContext): GraphQL[Task] =
    graphQL(NewsPostgresRepository.impl(transactor))

  def graphQL(
      repository: NewsRepository[Task]
  )(implicit ec: ExecutionContext): GraphQL[Task] =
    CalibanGraphQL.impl(repository)

}
