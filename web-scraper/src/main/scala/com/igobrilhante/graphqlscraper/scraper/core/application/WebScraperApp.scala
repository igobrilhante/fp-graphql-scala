package com.igobrilhante.graphqlscraper.scraper.core.application

import scala.util.control.NonFatal

import cats.ApplicativeError
import cats.data.EitherT
import cats.effect.kernel.Async
import cats.implicits._
import com.igobrilhante.graphqlscraper.core.entities.ScraperConfig
import com.igobrilhante.graphqlscraper.core.repository.NewsRepository
import com.igobrilhante.graphqlscraper.core.scraper.WebScraper
import org.typelevel.log4cats.Logger

/**
  * Typeclass that define the web scraper app bounded by context F.
  */
trait WebScraperApp[F[_]] {

  /**
    * Program implementation which runs and returns true in case of success or false otherwise
    */
  def program(): F[Boolean]

  /**
    * Error handling implementation for the scraper app.
    */
  def errorHandling(implicit
      F: ApplicativeError[F, Throwable],
      L: Logger[F]
  ): PartialFunction[Throwable, F[Boolean]] = {
    case error: ScraperException =>
      Logger[F].error(error)(error.getMessage) *> false.pure[F]
    case NonFatal(error) =>
      Logger[F].error(error)(error.getMessage) *> false.pure[F]
    case error: Throwable =>
      Logger[F].error(error)(error.getMessage) *> F.raiseError[Boolean](error)
  }

}

object WebScraperApp {

  def impl[F[_]](settings: ScraperConfig, repository: NewsRepository[F], scraper: WebScraper[F])(implicit
      F: Async[F],
      L: Logger[F]
  ): WebScraperApp[F] = new WebScraperApp[F] {

    def program(): F[Boolean] = {

      val computation = for {
        news   <- EitherT(scraper.scrape(settings.source))
        result <- EitherT(repository.insert(news))
      } yield result

      val computationResult = computation.value
        .flatMap {
          case Right(value) => value.pure[F]
          case Left(error) =>
            Logger[F].error(error)("Scrape failed") *> false.pure[F]
        }
        .handleErrorWith(errorHandling)

      computationResult
    }
  }

}
