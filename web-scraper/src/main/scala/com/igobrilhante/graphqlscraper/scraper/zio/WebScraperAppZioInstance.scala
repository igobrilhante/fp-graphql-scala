package com.igobrilhante.graphqlscraper.scraper.zio

import com.igobrilhante.graphqlscraper.core.entities.ScraperConfig
import com.igobrilhante.graphqlscraper.core.repository.NewsRepository
import com.igobrilhante.graphqlscraper.core.scraper.WebScraper
import com.igobrilhante.graphqlscraper.scraper.core.application.{LoggingF, WebScraperApp}
import zio.clock.Clock
import zio.duration.Duration
import zio.interop.catz.asyncRuntimeInstance
import zio.interop.catz.implicits._
import zio.{Schedule, Task, UIO}

/**
  * Implementation of WebScraperApp but using ZIO features. This is very similar to [[WebScraperApp]]. The main object
  * of this one is to exploit ZIO capabilities and compare the implementations.
  *
  * An interesting feature is the use of retry strategy in case the connection with the database is lost (or other
  * database related exception). In this case, we apply an exponential backoff strategy in hope to get the insert
  * completed.
  */
object WebScraperAppZioInstance {

  def impl(settings: ScraperConfig, repository: NewsRepository[Task], scraper: WebScraper[Task]): WebScraperApp[Task] =
    new WebScraperApp[Task] with LoggingF {

      private val retryInsertStrategy = Schedule
        .exponential(Duration.fromMillis(settings.retryDatabase * 1000))
        .addDelayM { _ =>
          val delay = Duration.fromMillis(100)
          (logger.info("Retrying insert into the database ...") *> UIO.succeed(delay))
            .catchAll(_ => UIO.succeed(delay))
        }

      override def program(): Task[Boolean] = {
        val computation = for {
          news <- scraper.scrape(settings.source).absolve
          result <- repository
            .insert(news)
            .retry(retryInsertStrategy)
        } yield result

        computation
          .flatMap {
            case Right(value) => Task.succeed(value)
            case Left(error) =>
              logger.error(error)("Scrape failed") *> Task.succeed(false)
          }
          .catchSome(errorHandling)
          .provideLayer(Clock.live)
      }
    }

}
