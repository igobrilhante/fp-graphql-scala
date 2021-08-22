package com.igobrilhante.graphqlscraper.core.repository


import com.igobrilhante.graphqlscraper.core.entities.News

trait NewsRepository[F[_]] {

  def insert(circles: List[News]): F[Either[Throwable, Boolean]]

  def list(): F[List[News]]

  def get(link: String): F[Option[News]]

  def delete(): F[Boolean]

  def count(): F[Long]

}
