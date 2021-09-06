package com.igobrilhante.graphqlscraper.core.sangria

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

import cats.effect.kernel.Async
import cats.implicits._
import com.igobrilhante.graphqlscraper.core.entities.GraphQLApiQuery
import com.igobrilhante.graphqlscraper.core.logging.Logging
import com.igobrilhante.graphqlscraper.core.schema.GraphQL
import io.circe.{Json, JsonObject}
import sangria.ast.Document
import sangria.execution.{ExceptionHandler, Executor, HandledException}
import sangria.marshalling.circe.{CirceInputUnmarshaller, _}
import sangria.parser.QueryParser
import sangria.schema.Schema

object SangriaGraphQL extends Logging {

  // Partially-applied constructor
  def impl[F[_]] = new SangriaGraphQLInternals[F]

  final class SangriaGraphQLInternals[F[_]] {

    def apply[A](schema: Schema[A, Unit], userContext: F[A])(implicit
        ec: ExecutionContext,
        F: Async[F]
    ): GraphQL[F] = new GraphQL[F] {

      override def query(q: GraphQLApiQuery): F[Either[Throwable, Json]] = {
        QueryParser.parse(q.query) match {
          case Success(ast) => exec(schema, ast, userContext, q.operationName, q.variables.getOrElse(JsonObject.empty))
          case Failure(exception) => F.pure(exception.asLeft[Json])
        }
      }

      def exec(
          schema: Schema[A, Unit],
          query: Document,
          userContext: F[A],
          operationName: Option[String],
          variables: JsonObject
      )(implicit ec: ExecutionContext): F[Either[Throwable, Json]] = {
        logger.debug("exec {}", query)
        userContext
          .flatMap { ctx =>
            F.async { (cb: Either[Throwable, Json] => Unit) =>
              Executor
                .execute(
                  schema = schema,
                  queryAst = query,
                  userContext = ctx,
                  variables = Json.fromJsonObject(variables),
                  operationName = operationName,
                  exceptionHandler = ExceptionHandler { case (_, e) ⇒
                    HandledException(e.getMessage)
                  }
                )
                .onComplete {
                  case Success(node)  => cb(node.asRight[Throwable])
                  case Failure(error) => cb(Left(error))
                }
              F.pure(None)
            }
          }
          .attempt
          .flatMap {
            case Right(json) => F.pure(json.asRight)
            case Left(err)   => F.pure(err.asLeft)
          }
      }
    }
  }
}
