package com.igobrilhante.graphqlscraper.core.caliban

import com.igobrilhante.graphqlscraper.core.entities.News
import com.igobrilhante.graphqlscraper.core.repository.{NewsRepository, NewsSubscription}
import zio.Task
import zio.stream.ZStream

/**
  * GraphQL Schema definition using Caliban.
  */
object CalibanGraphQLSchema {

  // NewsByLink argument definition
  case class NewsByLinkArgs(link: String)

  // Queries definitions
  case class NewsQueries(
      news: Task[List[News]],
      newsByLink: NewsByLinkArgs => Task[Option[News]]
  )

  case class Mutations(
      createNews: News => Task[Boolean]
  )

  case class Subscriptions(createdNews: ZStream[Any, Nothing, News])

  /**
    * Schema is represented by the case class using fields and operations.
    */
  def queries(repository: NewsRepository[Task]): NewsQueries = {
    NewsQueries(repository.list(), newsLink => repository.get(newsLink.link))
  }

  def mutations(repository: NewsRepository[Task]): Mutations =
    Mutations(createNews => repository.insert(List(createNews)).absolve)

  def subscriptions(stream: NewsSubscription[ZStream, Any, Nothing, News]): Subscriptions = Subscriptions(
    stream.subscribe()
  )

}
