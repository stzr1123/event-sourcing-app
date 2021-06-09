package dao

import akka.stream.scaladsl.{Sink, Source, SourceQueueWithComplete}
import akka.stream.{Materializer, OverflowStrategy}
import com.github.stzr1123.events.{LogRecord, TagCreated, TagDeleted}
import model._

import java.util.UUID
import scala.collection.mutable.{Map => MMap}
import scala.concurrent.{Future, Promise}

/*
  Not protected against concurrent read/writes.
 */
class InMemoryReadDao(implicit mat: Materializer) {
  import util.ThreadPools.CPU
  private object State {
    val tags = MMap.empty[UUID, Tag]
  }

  private val ReadQueue: SourceQueueWithComplete[ReadServiceRequest] = {
    Source.queue[ReadServiceRequest](bufferSize = 100,
      OverflowStrategy.dropNew).map {
      case GetStateRequest(callback) =>
        // why are we using the apply method of a function and
        // not just do a function call?
        callback.apply(getCurrentState)
      case UpdateStateRequest(event) =>
        updateState(event)
    }.to(Sink.ignore).run()
    }

  private def updateState(record: LogRecord): Unit = {
    record.action match {
      case TagCreated.actionName =>
        val event = record.data.as[TagCreated]
        State.tags += (event.id -> Tag(event.id, event.text))
      case TagDeleted.actionName =>
        val event = record.data.as[TagDeleted]
        State.tags -= event.id
      case _ => ()
    }
  }

  def processEvent(event: LogRecord): Future[Unit] = {
    // We do this map here to return a Future[Unit] instead of
    // a Future[QueueOfferResult]
    ReadQueue.offer(UpdateStateRequest(event)).map(_ => ())
  }

  def processEvents(events: Seq[LogRecord]): Future[Unit] = {
    Source(events).mapAsync(parallelism = 1) { event =>
      ReadQueue.offer(UpdateStateRequest(event))
    }.runWith(Sink.ignore).map(_ => ())
  }

  def getTags: Seq[Tag] = {
    State.tags.values.toList.sortWith(_.text < _.text)
  }

  private def getCurrentState: ApplicationState = {
    val tags = State.tags.toSeq.map { case (_, tag) => tag }
    ApplicationState(tags)
  }

  def getState: Future[ApplicationState] = {
    val promise = Promise[ApplicationState]()
    ReadQueue.offer(GetStateRequest(state => {
      promise.success(state)
    })).flatMap(_ => promise.future)
  }
}
