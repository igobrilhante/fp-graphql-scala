package com.igobrilhante.graphqlwebscraper.http.tests

import java.io.InputStream

import cats.effect.IO
import com.igobrilhante.graphqlscraper.core.entities.News
import com.igobrilhante.graphqlscraper.core.repository.NewsRepository
import io.circe.Json
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.implicits._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class HttpServiceSpec extends AnyFlatSpec with HttpSpecUtils with Matchers with BeforeAndAfterAll {

  val queryGraphql: InputStream        = getClass.getResourceAsStream("/query.json")
  val invalidQueryGraphql: InputStream = getClass.getResourceAsStream("/invalid-query.json")

  val repositoryIO: IO[(NewsRepository[IO], IO[Unit])] = inMemoryRepository[IO]()

  val data: List[News] = (0 until 10).toList.map(i => News(s"link $i", s"title $i"))

  override def beforeAll(): Unit = {
    super.beforeAll()
    insertData()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    cleanData()
  }

  "Http Service" should "query the graphql api" in {

    def body: EntityBody[IO] = fs2.io.readInputStream[IO](IO.pure(queryGraphql), 4096)
    val request              = makeRequest(body)
    val response             = doRequest(request)

    response.status shouldEqual Status.Ok

    val responseJson = response.as[Json].unsafeRunSync()
    val newsSet =
      responseJson.hcursor
        .downField("data")
        .downField("news")
        .as[Seq[News]]
        .toOption
        .getOrElse(Seq.empty)
        .toSet

    newsSet shouldEqual data.toSet
  }

  it should "handle invalid query" in {
    def body: EntityBody[IO] = fs2.io.readInputStream[IO](IO.pure(invalidQueryGraphql), 4096)
    val request              = makeRequest(body)
    val response             = doRequest(request)
    response.status shouldEqual Status.BadRequest
  }

  it should "handle not found query" in {
    val request  = Request[IO](method = Method.GET, uri = uri"/news")
    val response = doRequest(request)
    response.status shouldEqual Status.NotFound
  }

  def insertData(): Unit = {
    repositoryIO.map(_._1.insert(data)).unsafeRunSync()
  }

  def cleanData(): Unit = {
    repositoryIO.map(_._1.delete()).unsafeRunSync()
  }

  private def doRequest(request: Request[IO]) = (for {
    repository <- repositoryIO
    result     <- routesResource[IO](repository._1).use { routes => routes(request) }
  } yield result).unsafeRunSync()

}
