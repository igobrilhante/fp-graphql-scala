package com.igobrilhante.graphqlscraper.adapters.codecs

import cats.syntax.functor._
import com.igobrilhante.graphqlscraper.core.entities.{News, ScraperConfig}
import com.igobrilhante.graphqlscraper.core.scraper.{FileSource, ScraperSource, WebSource}
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

trait Codecs {

  implicit val encodeScraperSource: Encoder[ScraperSource] = Encoder.instance {
    case page @ WebSource(_)  => page.asJson
    case file @ FileSource(_) => file.asJson
  }

  implicit val decodeScraperSource: Decoder[ScraperSource] =
    List[Decoder[ScraperSource]](
      Decoder[WebSource].widen,
      Decoder[FileSource].widen
    ).reduceLeft(_ or _)

  implicit val scraperConfigDecoder: Decoder[ScraperConfig] = deriveDecoder
  implicit val scraperConfigEncoder: Encoder[ScraperConfig] = deriveEncoder

  implicit val newsDecoder: Decoder[News] = deriveDecoder
  implicit val newsEncoder: Encoder[News] = deriveEncoder

}

object Codecs extends Codecs
