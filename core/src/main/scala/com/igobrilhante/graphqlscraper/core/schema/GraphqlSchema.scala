package com.igobrilhante.graphqlscraper.core.schema

import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import com.igobrilhante.graphqlscraper.core.entities.News
import com.igobrilhante.graphqlscraper.core.repository.NewsRepository
import sangria.macros.derive._
import sangria.schema._

object GraphqlSchema {

  val NewsType: ObjectType[Unit, News] = deriveObjectType[Unit, News](
    ObjectTypeDescription("News"),
    DocumentField("link", "Link to access the news"),
    DocumentField("title", "Title of the news")
  )

  val LinkArg: Argument[String] = Argument("link", StringType, description = "Link of the news to filter")

  def QueryType[F[_]: Async](dispatcher: Dispatcher[F]): ObjectType[NewsRepository[F], Unit] =
    ObjectType(
      "Query",
      fields[NewsRepository[F], Unit](
        Field(
          "newsByLink",
          OptionType(NewsType),
          description = Some("Returns a product with specific `id`."),
          arguments = LinkArg :: Nil,
          resolve = c =>
            FutureValue {
              dispatcher.unsafeToFuture(c.ctx.get(c arg LinkArg))
            }
        ),
        Field(
          "news",
          ListType(NewsType),
          description = Some("Returns a list of all available products."),
          resolve = c =>
            FutureValue {
              dispatcher.unsafeToFuture(c.ctx.list())
            }
        )
      )
    )

  def schema[F[_]: Async](dispatcher: Dispatcher[F]): Schema[NewsRepository[F], Unit] =
    Schema(QueryType[F](dispatcher))

}
