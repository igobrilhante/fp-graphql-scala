package com.igobrilhante.graphqlscraper.core.entities

import com.igobrilhante.graphqlscraper.core.scraper.ScraperSource

case class ScraperConfig(
    source: ScraperSource,
    waitingTime: Long,
    retryDelay: Long,
    retryDatabase: Long,
    simulateScrollDown: Int
)

object ScraperConfig {
  def apply(source: ScraperSource): ScraperConfig =
    ScraperConfig(source, waitingTime = 60, retryDelay = 5, retryDatabase = 10, simulateScrollDown = 10)
}
