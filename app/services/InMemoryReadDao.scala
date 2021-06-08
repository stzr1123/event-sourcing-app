package services

import com.github.stzr1123.events.{LogRecord, TagCreated, TagDeleted}
import model.Tag

import scala.collection.mutable.{Map => MMap}
import java.util.UUID

/*
  Not protected against concurrent read/writes.
 */
class InMemoryReadDao {
  private object State {
    val tags = MMap.empty[UUID, Tag]
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

  def processEvents(events: Seq[LogRecord]): Unit = {
    events.foreach(updateState)
  }

  def getTags: Seq[Tag] = {
    State.tags.values.toList.sortWith(_.text < _.text)
  }
}
