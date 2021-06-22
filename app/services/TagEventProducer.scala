package services

import com.github.stzr1123.events.{EventData, LogRecord, TagCreated, TagDeleted}
import dao.LogDao
import messaging.IMessageProcessingRegistry
import model.Tag

import java.time.ZonedDateTime
import java.util.UUID
import scala.concurrent.Future

class TagEventProducer(registry: IMessageProcessingRegistry) {
  private val producer = registry.createProducer("tags")

  def createTag(text: String, createdBy: UUID): Unit = {
    val tagId = UUID.randomUUID()
    val event = TagCreated(tagId, text, createdBy)
    val record = LogRecord.fromEvent(event)
    producer.send(record.encode)
  }

  def deleteTag(tagId: UUID, deletedBy: UUID): Unit = {
    val event = TagDeleted(tagId, deletedBy)
    val record = LogRecord.fromEvent(event)
    producer.send(record.encode)
  }
}
