package com.igobrilhante.graphqlscraper.scraper.io

import cats.data.EitherT
import cats.effect.IO
import com.igobrilhante.graphqlscraper.core.entities.{News, ScraperConfig}
import com.igobrilhante.graphqlscraper.core.scraper.{FileSource, ScraperSource, WebScraper, WebSource}
import com.igobrilhante.graphqlscraper.scraper.core.application.{LoggingF, WebScraperF}
import org.typelevel.log4cats.Logger

/**
  * Typeclass instance based on Cats Effect IO.
  */
object WebScraperIO {

  def impl(settings: ScraperConfig): WebScraper[IO] = new WebScraper[IO] with WebScraperF[IO] with LoggingF {

    def scrape(source: ScraperSource): IO[Either[Throwable, List[News]]] = {
      source match {
        case WebSource(page)  => scrapeFromWeb(page)
        case FileSource(file) => scrapeFromFile(file)
      }
    }

    private def scrapeFromWeb(page: String): IO[Either[Throwable, List[News]]] = getBrowserHtmlUnitBrowser.use {
      browser =>
        Logger[IO].info(s"Scrape web page from  $page") *>
          (for {
            doc    <- EitherT(getPage(browser, page).attempt)
            _      <- EitherT.right(simulateScrollDown(doc, settings.simulateScrollDown))
            result <- EitherT(scrapeDoc(doc))
          } yield result).value
    }

    private def scrapeFromFile(file: String): IO[Either[Throwable, List[News]]] = getJsoupBrowser.use { browser =>
      (for {
        doc    <- EitherT(parseFile(browser, file))
        result <- EitherT(scrapeDoc(doc))
        _      <- EitherT.right[Throwable](Logger[IO].info(s"Retrieved ${result.length} news"))
      } yield result).value
    }

  }
}
