package com.igobrilhante.graphqlscraper.core.repository

import com.igobrilhante.graphqlscraper.core.entities.News
import zio.Schedule
import zio.duration.durationInt
import zio.stream.ZStream

trait NewsSubscription[Stream[_, _, _], R, E, A] {

  def subscribe(): Stream[R, E, A]

}

class NewsSubscriptionImpl extends NewsSubscription[ZStream, Any, Nothing, News] {

  override def subscribe(): ZStream[Any, Nothing, News] = {

    val data = (1 to 100).toIterable.map(i => News(s"link $i", s"title $i"))

    ZStream
      .fromIterable(data)
      .schedule(Schedule.duration(5.second))
      .forever
      .provideLayer(zio.clock.Clock.live)

  }

}
