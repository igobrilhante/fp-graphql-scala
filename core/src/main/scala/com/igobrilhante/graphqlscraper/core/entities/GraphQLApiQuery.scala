package com.igobrilhante.graphqlscraper.core.entities

import cats.effect.IO
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder, JsonObject}
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

case class GraphQLApiQuery(
    query: String,
    variables: Option[JsonObject],
    operationName: Option[String] = None
)

object GraphQLApiQuery {
  implicit val decoder: Decoder[GraphQLApiQuery] = deriveDecoder
  implicit val encoder: Encoder[GraphQLApiQuery] = deriveEncoder

  implicit val ofDecoder: EntityDecoder[IO, GraphQLApiQuery] = jsonOf[IO, GraphQLApiQuery]

}
