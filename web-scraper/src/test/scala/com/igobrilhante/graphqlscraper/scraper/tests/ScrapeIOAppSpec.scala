package com.igobrilhante.graphqlscraper.scraper.tests

import scala.language.postfixOps

import cats.effect.IO
import com.igobrilhante.graphqlscraper.core.entities.ScraperConfig
import com.igobrilhante.graphqlscraper.core.scraper.{FileSource, WebScraper}
import com.igobrilhante.graphqlscraper.core.tests.SpecUtils
import com.igobrilhante.graphqlscraper.scraper.io.WebScraperIOApp.logger
import com.igobrilhante.graphqlscraper.scraper.core.application.{ConnectHostException, WebScraperApp}
import com.igobrilhante.graphqlscraper.scraper.io.WebScraperIO
import com.igobrilhante.graphqlscraper.scraper.tests.SpecData.{expectedNews, invalidUrl}
import org.mockito.MockitoSugar
import org.mockito.cats.MockitoCats
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ScrapeIOAppSpec
    extends AnyFlatSpec
    with SpecUtils
    with Matchers
    with BeforeAndAfterEach
    with MockitoSugar
    with MockitoCats {

  val file     = FileSource(getClass.getResource("/nytimes.com.html").getPath)
  val settings = ScraperConfig(file)
  val scraper  = WebScraperIO.impl(settings)

  "Scraper with Cats Effect" should "scrape the html from file" in {
    val (repository, _) = inMemoryRepositoryResource[IO].allocated.unsafeRunSync()

    val program = WebScraperApp.impl[IO](settings, repository, scraper)
    val result  = program.program().unsafeRunSync()

    result shouldEqual true

    val newsList = repository.list().unsafeRunSync()

    newsList should contain(expectedNews)

  }

  it should "handle connection issues" in {
    val mockScraper     = mock[WebScraper[IO]]
    val (repository, _) = inMemoryRepositoryResource[IO].allocated.unsafeRunSync()
    val settings        = ScraperConfig(invalidUrl)

    whenF(mockScraper.scrape(settings.source)) thenReturn Left(ConnectHostException(invalidUrl.page))

    val program = WebScraperApp.impl[IO](settings, repository, mockScraper)
    val result  = program.program().unsafeRunSync()

    result shouldEqual false
  }

  it should "handle non fatal exceptions" in {
    val mockScraper     = mock[WebScraper[IO]]
    val (repository, _) = inMemoryRepositoryResource[IO].allocated.unsafeRunSync()
    val settings        = ScraperConfig(invalidUrl)

    whenF(mockScraper.scrape(settings.source)) thenReturn Left(new RuntimeException)

    val program = WebScraperApp.impl[IO](settings, repository, mockScraper)
    val result  = program.program().unsafeRunSync()

    result shouldEqual false
  }

}
