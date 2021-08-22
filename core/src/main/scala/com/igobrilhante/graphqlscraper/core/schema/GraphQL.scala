package com.igobrilhante.graphqlscraper.core.schema

import com.igobrilhante.graphqlscraper.core.entities.GraphQLApiQuery
import io.circe.Json

trait GraphQL[F[_]] {

  def query(q: GraphQLApiQuery): F[Either[Throwable, Json]]

}
