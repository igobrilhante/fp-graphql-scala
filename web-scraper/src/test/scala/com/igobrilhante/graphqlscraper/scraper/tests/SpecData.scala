package com.igobrilhante.graphqlscraper.scraper.tests

import com.igobrilhante.graphqlscraper.core.entities.News
import com.igobrilhante.graphqlscraper.core.scraper.WebSource

object SpecData {

  val expectedNews: News = News(
    "https://www.nytimes.com/live/2021/08/19/world/taliban-afghanistan-news/",
    "Protests Spread to Kabul as Taliban Struggle to Govern"
  )

  val invalidUrl = WebSource("https://www.nytimes12312321.com/")

}
