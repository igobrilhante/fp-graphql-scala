package com.igobrilhante.graphqlscraper.core.entities

import io.circe.{Encoder, Json}

trait ApiResponse

final case class ApiErrorException(message: String) extends Exception

final case class ApiError(error: Throwable) extends ApiResponse

object ApiError {
  implicit val encoder: Encoder[ApiError] = (error: ApiError) =>
    Json.obj(
      (
        "errors",
        Json.fromValues(
          Seq(
            Json.obj(("message", Json.fromString(error.error.getMessage)))
          )
        )
      )
    )
}
