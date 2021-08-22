package com.igobrilhante.graphqlwebscraper.integration.tests

import java.io.InputStream

import cats.effect.IO
import com.igobrilhante.graphqlscraper.core.entities.{News, ScraperConfig}
import com.igobrilhante.graphqlscraper.core.scraper.WebSource
import com.igobrilhante.graphqlscraper.scraper.io.WebScraperIOApp.logger
import com.igobrilhante.graphqlscraper.scraper.core.application.WebScraperApp
import com.igobrilhante.graphqlscraper.scraper.io.WebScraperIO
import com.igobrilhante.graphqlwebscraper.http.tests.HttpSpecUtils
import io.circe.Json
import org.http4s.circe.jsonDecoder
import org.http4s.{EntityBody, Status}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class IntegrationSpec extends AnyFlatSpec with HttpSpecUtils with Matchers with BeforeAndAfterAll {

  val WebPageSource             = WebSource("https://www.nytimes.com/")
  val settings                  = ScraperConfig(WebPageSource)
  val queryGraphql: InputStream = getClass.getResourceAsStream("/query.json")
  val scraper                   = WebScraperIO.impl(settings)

  override def beforeAll(): Unit = {
    repositoryResource[IO]
      .use { _.delete() }
      .unsafeRunSync()
  }

  it should "scrape the html and store the news in the database" in {

    val result = repositoryResource[IO]
      .use { repository =>
        WebScraperApp.impl[IO](settings, repository, scraper).program()
      }
      .unsafeRunSync()

    result shouldEqual true

    val storedList = repositoryResource[IO]
      .use { _.list() }
      .unsafeRunSync()

    storedList should not be empty

  }

  it should "query the graphql api and get the expected number of news" in {

    val expectedTotalNews = repositoryResource[IO].use { _.count() }.unsafeRunSync()

    def body: EntityBody[IO] = fs2.io.readInputStream[IO](IO.pure(queryGraphql), 4096)
    val request              = makeRequest(body)
    val response = (for {
      repository <- repositoryResource[IO]
      routes     <- routesResource[IO](repository)
    } yield routes).use { routes => routes(request) }.unsafeRunSync()

    response.status shouldEqual Status.Ok

    val responseJson = response.as[Json].unsafeRunSync()
    val newsJson =
      responseJson.hcursor
        .downField("data")
        .downField("news")
        .as[Seq[News]]
        .toOption
        .getOrElse(Seq.empty)

    newsJson.size shouldEqual expectedTotalNews

  }

}
