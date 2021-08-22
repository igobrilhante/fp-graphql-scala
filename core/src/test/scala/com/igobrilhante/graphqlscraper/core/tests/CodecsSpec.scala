package com.igobrilhante.graphqlscraper.core.tests

import com.igobrilhante.graphqlscraper.adapters.codecs.Codecs
import com.igobrilhante.graphqlscraper.core.entities.ScraperConfig
import com.igobrilhante.graphqlscraper.core.scraper.{FileSource, WebSource}
import com.typesafe.config.ConfigFactory
import io.circe.Json
import io.circe.config.parser
import io.circe.syntax.EncoderOps
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CodecsSpec extends AnyFlatSpec with Matchers with Codecs {

  "Codecs Spec" should "encode ScraperConfig with web source" in {
    ScraperConfig(WebSource("some url")).asJson shouldBe a[Json]
    ScraperConfig(FileSource("filename")).asJson shouldBe a[Json]
  }

  it should "decode HOCON into scraper config with WebSource" in {
    val config = ConfigFactory.load("websource.conf")
    val result = parser.decodePath[ScraperConfig](config, "scraper")
    type ExpectedType = Right[Throwable, ScraperConfig]

    result shouldBe a[ExpectedType]

  }

  it should "decode HOCON into scraper config with FileSource" in {
    val config = ConfigFactory.load("filesource.conf")
    val result = parser.decodePath[ScraperConfig](config, "scraper")
    type ExpectedType = Right[Throwable, ScraperConfig]

    result shouldBe a[ExpectedType]

  }

}
