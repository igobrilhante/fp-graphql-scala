package com.igobrilhante.graphqlscraper.scraper.core.application

/**
 * Scraper exception definitions.
 */
sealed trait ScraperException extends Exception

case class UnexpectedScraperException(cause: Option[Exception]) extends ScraperException {
  override def getMessage: String = "Unexpected scraper exception"
}
case class ConnectHostException(host: String) extends ScraperException {
  override def getMessage: String = s"Connection to host exception: $host"
}
