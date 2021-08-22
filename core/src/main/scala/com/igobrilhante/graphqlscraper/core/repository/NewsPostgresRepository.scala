package com.igobrilhante.graphqlscraper.core.repository

import cats.effect.kernel.Async
import cats.syntax.all._
import com.igobrilhante.graphqlscraper.core.entities.News
import doobie.Transactor
import doobie.implicits._
import doobie.quill.DoobieContext
import io.getquill.{idiom => _, _}

object NewsPostgresRepository {

  def impl[F[_]: Async](xa: Transactor[F]): NewsRepository[F] = new NewsRepository[F] {

    val ctx = new DoobieContext.Postgres(PluralizedTableNames)
    import ctx._

    private val table = quote {
      querySchema[News]("headlines")
    }

    def insert(circles: List[News]): F[Either[Throwable, Boolean]] = {
      val q = quote {
        liftQuery(circles).foreach(c => table.insert(c).onConflictIgnore(_.link))
      }
      run(q).transact[F](xa).map(_ => true.asRight)
    }

    def list(): F[List[News]] = {
      run(table).transact(xa)
    }

    def get(link: String): F[Option[News]] = {
      val q = quote {
        table.filter(_.link == lift(link))
      }
      run(q).map(_.headOption).transact[F](xa)
    }

    def delete(): F[Boolean] = {
      run(table.delete).transact[F](xa).map(_ => true)
    }

    def count(): F[Long] = {
      run(table.size).transact[F](xa)
    }
  }

}
