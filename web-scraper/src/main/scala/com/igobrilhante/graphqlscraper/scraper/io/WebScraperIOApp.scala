package com.igobrilhante.graphqlscraper.scraper.io

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.igobrilhante.graphqlscraper.adapters.codecs.Codecs
import com.igobrilhante.graphqlscraper.core.entities.ScraperConfig
import com.igobrilhante.graphqlscraper.core.repository.{NewsPostgresRepository, NewsRepository, RepositoryComponents}
import com.igobrilhante.graphqlscraper.scraper.core.application.{LoggingF, WebScraperApp}
import com.typesafe.config.ConfigFactory
import io.circe.config.parser
import org.typelevel.log4cats.Logger

/**
  * Scraper application using Cats Effect IO.
  */
object WebScraperIOApp extends IOApp with RepositoryComponents with Codecs with LoggingF {

  def repositoryResource: Resource[IO, NewsRepository[IO]] = {
    for {
      xa <- transactor[IO]()
      repo = NewsPostgresRepository.impl[IO](xa)
    } yield repo
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val config = ConfigFactory.load()

    repositoryResource.use { repository =>
      for {
        _        <- Logger[IO].info("Running WebScraper with Cats Effect")
        settings <- parser.decodePathF[IO, ScraperConfig](config, "scraper")
        scraper = WebScraperIO.impl(settings)
        app     = WebScraperApp.impl[IO](settings, repository, scraper)
        _ <- {
          app.program() *>
            Logger[IO].info(s"Waiting for ${settings.waitingTime} seconds to the next execution ...") *>
            IO.sleep(FiniteDuration(settings.waitingTime, TimeUnit.SECONDS))
        }.foreverM
        res <- IO.never.as(ExitCode.Success)
      } yield res
    }

  }
}
