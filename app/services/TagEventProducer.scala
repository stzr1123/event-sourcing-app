package services

import com.github.stzr1123.events.{LogRecord, TagCreated, TagDeleted}
import messaging.IMessageProcessingRegistry

import java.util.UUID

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
