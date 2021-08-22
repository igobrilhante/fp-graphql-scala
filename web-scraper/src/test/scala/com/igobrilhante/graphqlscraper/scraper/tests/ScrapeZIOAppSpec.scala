package com.igobrilhante.graphqlscraper.scraper.tests

import scala.language.postfixOps

import com.igobrilhante.graphqlscraper.core.entities.{News, ScraperConfig}
import com.igobrilhante.graphqlscraper.core.scraper.{FileSource, WebScraper}
import com.igobrilhante.graphqlscraper.core.tests.SpecUtils
import com.igobrilhante.graphqlscraper.scraper.core.application.ConnectHostException
import com.igobrilhante.graphqlscraper.scraper.tests.SpecData.{expectedNews, invalidUrl}
import com.igobrilhante.graphqlscraper.scraper.zio.{WebScraperAppZioInstance, WebScraperZio}
import org.mockito.MockitoSugar
import org.mockito.cats.MockitoCats
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio.Task
import zio.interop.catz._

class ScrapeZIOAppSpec
    extends AnyFlatSpec
    with SpecUtils
    with Matchers
    with BeforeAndAfterEach
    with MockitoSugar
    with MockitoCats {

  val file        = FileSource(getClass.getResource("/nytimes.com.html").getPath)
  val invalidHtml = FileSource(getClass.getResource("/invalid.html").getPath)
  val config      = ScraperConfig(file)
  val scraper     = WebScraperZio.impl(config)

  "Scraper with ZIO" should "scrape the html from file" in {

    val program = inMemoryRepositoryResource[Task].use { inMemoryRepository =>
      WebScraperAppZioInstance
        .impl(config, inMemoryRepository, scraper)
        .program()
    }
    val result = unsafeRunSync(false)(program)
    result shouldEqual true

    val storedList = unsafeRunSync(List.empty[News])(
      inMemoryRepositoryResource[Task]
        .use { _.list() }
    )

    storedList should contain(expectedNews)
  }

  it should "handle connection issues" in {
    val mockScraper = mock[WebScraper[Task]]
    val settings    = ScraperConfig(invalidUrl)
    whenF(mockScraper.scrape(settings.source)) thenReturn Left(ConnectHostException(invalidUrl.page))

    val program = inMemoryRepositoryResource[Task].use { inMemoryRepository =>
      WebScraperAppZioInstance
        .impl(settings, inMemoryRepository, mockScraper)
        .program()
    }
    val result = unsafeRunSync(false)(program)

    result shouldEqual false
  }

  it should "handle non fatal exceptions" in {
    val mockScraper = mock[WebScraper[Task]]
    val settings    = ScraperConfig(invalidUrl)
    whenF(mockScraper.scrape(settings.source)) thenReturn Left(new RuntimeException)

    val program = inMemoryRepositoryResource[Task].use { inMemoryRepository =>
      WebScraperAppZioInstance
        .impl(settings, inMemoryRepository, mockScraper)
        .program()
    }
    val result = unsafeRunSync(false)(program)

    result shouldEqual false
  }

  it should "handle invalid html" in {
    val settings = ScraperConfig(invalidHtml)
    val program = inMemoryRepositoryResource[Task].use { inMemoryRepository =>
      WebScraperAppZioInstance
        .impl(settings, inMemoryRepository, scraper)
        .program()
    }
    val result = unsafeRunSync(false)(program)
    result shouldEqual true
  }

  def unsafeRunSync[A](orElse: A)(computation: Task[A]): A = {
    zioRuntime.unsafeRunSync(computation).toEither.getOrElse(orElse)
  }

  def unsafeRunSyncEither[A](orElse: A)(computation: Task[Either[Throwable, A]]): A =
    unsafeRunSync(orElse)(computation.absolve)

  def unsafeRunSyncEither[A](computation: Task[Either[Throwable, A]]): Either[Throwable, A] =
    zioRuntime.unsafeRunSync(computation.absolve).toEither

}
