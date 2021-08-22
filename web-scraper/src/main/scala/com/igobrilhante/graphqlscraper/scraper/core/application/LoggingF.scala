package com.igobrilhante.graphqlscraper.scraper.core.application

import cats.effect.Sync
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

/**
  * Logging capabilities using a context F.
  */
trait LoggingF {

  implicit def logger[F[_]: Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger[F]

}
