package com.igobrilhante.graphqlscraper.scraper.zio

import cats.effect.Async
import com.igobrilhante.graphqlscraper.core.entities.{News, ScraperConfig}
import com.igobrilhante.graphqlscraper.core.scraper.{FileSource, ScraperSource, WebScraper, WebSource}
import com.igobrilhante.graphqlscraper.scraper.core.application.{LoggingF, WebScraperF}
import net.ruippeixotog.scalascraper.browser.Browser
import zio.clock.Clock
import zio.duration.Duration
import zio.interop.catz.asyncRuntimeInstance
import zio.interop.catz.implicits._
import zio.{Schedule, Task, URIO}

/**
  * Typeclass instance based on ZIO.
  */
object WebScraperZio {

  def impl(config: ScraperConfig): WebScraper[Task] = new WebScraper[Task] with WebScraperF[Task] with LoggingF {

    override def scrape(source: ScraperSource): Task[Either[Throwable, List[News]]] = {
      source match {
        case WebSource(page)  => scrapeWeb(page)
        case FileSource(file) => scrapeFromFile(file)
      }
    }

    private def scrapeFromFile(file: String): Task[Either[Throwable, List[News]]] = {
      getJsoupBrowser.use { browser =>
        for {
          doc    <- parseFile(browser, file).absolve
          result <- scrapeDoc(doc)
        } yield result
      }
    }

    private def scrapeWeb(page: String): Task[Either[Throwable, List[News]]] = {
      getBrowserHtmlUnitBrowser.use { browser =>
        for {
          doc    <- getPage(browser, page)
          _      <- simulateScrollDown(doc, config.simulateScrollDown)
          result <- scrapeDoc(doc)
        } yield result
      }
    }

    /**
      * Override default implementation to apply a retry policy in case the page cannot be obtained. Take advantage of
      * retry operation using an exponential backoff strategy to give another possibility to get the page.
      *
      * It tries only once, if it can not succeed, the exception is raised.
      */
    override def getPage(browser: Browser, page: String)(implicit F: Async[Task]): Task[browser.DocumentType] = {
      val runtime = asyncRuntimeInstance

      // retry strategy definition
      def retryStrategy = {
        val duration  = Duration.fromMillis(config.retryDelay * 1000)
        val durationF = URIO.succeed(duration)
        Schedule.once.addDelayM { _ =>
          (logger(runtime).info(s"Retrying get the page $page in ${duration.toSeconds} seconds ...") *> durationF)
            .catchAll(_ => durationF)
        }
      }

      super
        .getPage(browser, page)(runtime)
        .retry(retryStrategy)
        .provideLayer(Clock.live)
    }
  }
}
