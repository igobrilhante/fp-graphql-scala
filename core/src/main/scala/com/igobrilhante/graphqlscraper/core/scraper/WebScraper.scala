package com.igobrilhante.graphqlscraper.core.scraper

import com.igobrilhante.graphqlscraper.core.entities.News

/**
  * Define the type of source accepted by the scraper.
  */
sealed trait ScraperSource

/**
  * Source from a webpage
  */
case class WebSource(page: String) extends ScraperSource

/**
  * Source from a file
  */
case class FileSource(file: String) extends ScraperSource

/**
  * Typeclass for the web scraper.
  */
trait WebScraper[F[_]] {

  /**
    * Return a list of [[News]] after scraping a source [[ScraperSource]].
    */
  def scrape(source: ScraperSource): F[Either[Throwable, List[News]]]

}
