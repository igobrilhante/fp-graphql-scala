package com.igobrilhante.graphqlscraper.core.logging

import org.slf4j.{Logger, LoggerFactory}

trait Logging {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

}
