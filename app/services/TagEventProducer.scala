package services

import com.github.stzr1123.events.{EventData, LogRecord, TagCreated, TagDeleted}
import dao.LogDao
import model.Tag

import java.time.ZonedDateTime
import java.util.UUID
import scala.concurrent.Future

class TagEventProducer(logDao: LogDao, readService: ReadService) {
  import util.ThreadPools.CPU
  private def createLogRecord(eventData: EventData): LogRecord = {
    LogRecord(UUID.randomUUID(), eventData.action, eventData.json, ZonedDateTime.now())
  }

  private def adjustReadState(event: LogRecord): Future[Seq[Tag]] = {
    readService.adjustState(event).flatMap { _ =>
      readService.getState.map(_.tags)
    }
  }

  def createTag(text: String, createdBy: UUID): Future[Seq[Tag]] = {
    val tagId = UUID.randomUUID()
    val event = TagCreated(tagId, text, createdBy)
    val record = createLogRecord(event)
    // this is bad for obvious reasons. we're sending the same update
    // to two different places
    logDao.insertLogRecord(record).flatMap { _ =>
      adjustReadState(record)
    }
  }

  def deleteTag(tagId: UUID, deletedBy: UUID): Future[Seq[Tag]] = {
    val event = TagDeleted(tagId, deletedBy)
    val record = createLogRecord(event)
    logDao.insertLogRecord(record).flatMap { _ =>
      adjustReadState(record)
    }
  }
}
