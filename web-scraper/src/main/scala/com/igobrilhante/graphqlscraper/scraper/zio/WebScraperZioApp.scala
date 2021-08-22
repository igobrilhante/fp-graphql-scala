package com.igobrilhante.graphqlscraper.scraper.zio

import cats.effect.Resource
import com.igobrilhante.graphqlscraper.adapters.codecs.Codecs
import com.igobrilhante.graphqlscraper.core.entities.ScraperConfig
import com.igobrilhante.graphqlscraper.core.repository.{NewsPostgresRepository, NewsRepository, RepositoryComponents}
import com.igobrilhante.graphqlscraper.scraper.core.application.LoggingF
import com.typesafe.config.ConfigFactory
import io.circe.config.parser
import org.typelevel.log4cats.Logger
import zio.clock.Clock
import zio.duration.Duration
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.{Task, URIO, ZIO}

/**
  * Scraper application using ZIO.
  */
object WebScraperZioApp extends zio.App with RepositoryComponents with Codecs with LoggingF {

  def repositoryResource: Resource[Task, NewsRepository[Task]] = {
    for {
      xa <- transactor[Task]()
      repo = NewsPostgresRepository.impl[Task](xa)
    } yield repo
  }

  override def run(args: List[String]): URIO[zio.ZEnv, zio.ExitCode] = {
    val config = ConfigFactory.load()

    repositoryResource.use { repository =>
      for {
        _        <- Logger[Task].info("Running WebScraper with ZIO")
        settings <- parser.decodePathF[Task, ScraperConfig](config, "scraper")
        scraper = WebScraperZio.impl(settings)
        app     = WebScraperAppZioInstance.impl(settings, repository, scraper)
        _ <- {
          app.program() *>
            Logger[Task].info(s"Waiting for ${settings.waitingTime} seconds to the next execution ...") *>
            ZIO.sleep(Duration.fromMillis(settings.waitingTime * 1000))
        }
          .provideLayer(Clock.live)
          .forever
      } yield ()
    }.exitCode
  }
}
