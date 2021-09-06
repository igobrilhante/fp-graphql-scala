package com.igobrilhante.graphqlscraper.core.caliban

import com.igobrilhante.graphqlscraper.core.entities.News
import com.igobrilhante.graphqlscraper.core.repository.NewsRepository
import zio.Task

object CalibanGraphQLSchema {

  case class NewsByLinkArgs(link: String)

  case class NewsQueries(
      news: Task[List[News]],
      newsByLink: NewsByLinkArgs => Task[Option[News]]
  )

  def schema(repository: NewsRepository[Task]): NewsQueries = {
    NewsQueries(repository.list(), newsLink => repository.get(newsLink.link))
  }

}
