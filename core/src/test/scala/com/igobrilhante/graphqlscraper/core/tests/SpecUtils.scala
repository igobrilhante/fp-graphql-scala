package com.igobrilhante.graphqlscraper.core.tests

import cats.effect.Resource
import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import com.igobrilhante.graphqlscraper.adapters.codecs.Codecs
import com.igobrilhante.graphqlscraper.core.repository.{InMemoryRepository, NewsPostgresRepository, NewsRepository}
import com.igobrilhante.graphqlscraper.core.sangria.SangriaComponents
import com.igobrilhante.graphqlscraper.core.schema.GraphQL
import com.typesafe.config.{Config, ConfigFactory}
import doobie.ExecutionContexts
import doobie.hikari.HikariTransactor

trait SpecUtils extends SangriaComponents with Codecs {

  implicit val ec          = scala.concurrent.ExecutionContext.Implicits.global
  implicit val catsRuntime = cats.effect.unsafe.implicits.global
  implicit val zioRuntime  = zio.Runtime.default

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

  def graphQLResource[F[_]: Async](repository: NewsRepository[F]): Resource[F, GraphQL[F]] = for {
    dispatcher <- Dispatcher[F].map(identity)
    graphQLInstance = graphQL(dispatcher, repository)
  } yield graphQLInstance

  def repositoryResource[F[_]: Async]: Resource[F, NewsRepository[F]] = transactor()
    .map { xa => NewsPostgresRepository.impl[F](xa) }

  def inMemoryRepositoryResource[F[_]: Async]: Resource[F, NewsRepository[F]] =
    Resource.pure(InMemoryRepository.impl[F]())

  def inMemoryRepository[F[_]: Async](): F[(NewsRepository[F], F[Unit])] = inMemoryRepositoryResource.allocated

}
