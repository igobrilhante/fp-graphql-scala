package com.igobrilhante.graphqlscraper.core.repository

import cats.effect.Async
import cats.implicits._
import com.igobrilhante.graphqlscraper.core.entities.News

object InMemoryRepository {

  private var storage: Map[String, News] = Map.empty

  def impl[F[_]: Async](): NewsRepository[F] = new NewsRepository[F] {

    override def insert(circles: List[News]): F[Either[Throwable, Boolean]] = {
      storage ++= circles.map(c => (c.link, c)).toMap
      true.pure[F].attempt
    }

    override def list(): F[List[News]] = storage.values.toList.pure[F]

    override def get(link: String): F[Option[News]] = storage.get(link).pure[F]

    override def delete(): F[Boolean] = {
      storage = Map.empty
      true.pure[F]
    }

    override def count(): F[Long] = storage.size.toLong.pure[F]

  }

}
