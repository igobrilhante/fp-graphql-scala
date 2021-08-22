package com.igobrilhante.graphqlscraper.core.repository

import cats.effect.{Async, Resource}
import com.typesafe.config.{Config, ConfigFactory}
import doobie.ExecutionContexts
import doobie.hikari.HikariTransactor

trait RepositoryComponents {

  // Construct a transactor for connecting to the database.
  def transactor[F[_]: Async](
  ): Resource[F, HikariTransactor[F]] = {
    val conf: Config     = ConfigFactory.load()
    val dbConfig: Config = conf.getConfig("db.postgres")

    ExecutionContexts.fixedThreadPool[F](dbConfig.getInt("max-pool-size")).flatMap { ce =>
      HikariTransactor.newHikariTransactor(
        dbConfig.getString("driver"),
        dbConfig.getString("url"),
        dbConfig.getString("username"),
        dbConfig.getString("password"),
        ce
      )
    }
  }

}
