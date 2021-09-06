package com.igobrilhante.graphqlscraper.core.caliban

import caliban.GraphQL.graphQL
import caliban.GraphQLResponse._
import caliban.{GraphQLResponse, InputValue, RootResolver}
import cats.implicits._
import com.igobrilhante.graphqlscraper.core.entities.GraphQLApiQuery
import com.igobrilhante.graphqlscraper.core.repository.NewsRepository
import com.igobrilhante.graphqlscraper.core.schema.GraphQL
import io.circe.syntax._
import io.circe.{Json, JsonObject}
import zio.{Task, ZIO}

object CalibanGraphQL {

  def impl(repository: NewsRepository[Task]): GraphQL[Task] = new GraphQL[Task] {
    private val api = graphQL(RootResolver(CalibanGraphQLSchema.schema(repository)))

    println(api.render)

    override def query(q: GraphQLApiQuery): Task[Either[Throwable, Json]] = {
      exec(q)
    }

    private def exec(q: GraphQLApiQuery) = {
      val response = for {
        interpreter <- api.interpreter
        variables   <- parseVariables(q.variables)
        result      <- interpreter.execute(q.query, operationName = q.operationName, variables = variables)
        response = handleResponse(result)
      } yield response

      response
    }

    /**
     * Handle Caliban response.
     */
    private def handleResponse(response: GraphQLResponse[Throwable]): Either[Throwable, Json] = {
      response.errors match {
        case Nil        => response.asJson.asRight
        case error :: _ => error.asLeft
      }
    }

    /**
      * Parse graphql variables.
      */
    private def parseVariables(optJson: Option[JsonObject]): Task[Map[String, InputValue]] = {

      // decode json value into InputValue. The computation is wrapped by a Task.
      def decodeJson(data: (String, Json)) =
        Task.fromEither {
          val (key, json) = data
          InputValue.circeDecoder.decodeJson(json).map((key, _))
        }

      // decode json object
      def handleJsonObject(json: JsonObject) = {
        val tasks = json.toList.map(decodeJson)
        ZIO.collectAllPar(tasks)
      }

      // handle case no variables are provided
      def handleEmptyVariables: Task[List[(String, InputValue)]] = ZIO.effectTotal(List.empty[(String, InputValue)])

      for {
        tasks <- optJson.fold(handleEmptyVariables)(handleJsonObject)
        result = tasks.toMap
      } yield result

    }
  }

}
