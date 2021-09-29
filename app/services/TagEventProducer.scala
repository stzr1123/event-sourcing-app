package services

import com.github.stzr1123.events.{LogRecord, TagCreated, TagDeleted}
import messaging.IMessageProcessingRegistry

import java.util.UUID
import scala.concurrent.Future

class TagEventProducer(registry: IMessageProcessingRegistry, validationService: ValidationService) {
  private val producer = registry.createProducer("tags")

  def createTag(text: String, createdBy: UUID): Future[Option[String]] = {
    val tagId = UUID.randomUUID()
    val event = TagCreated(tagId, text, createdBy)
    val record = LogRecord.fromEvent(event)
    validationService.validateAndSend(createdBy, record, producer)
  }

  def deleteTag(tagId: UUID, deletedBy: UUID): Future[Option[String]] = {
    val event = TagDeleted(tagId, deletedBy)
    val record = LogRecord.fromEvent(event)
    validationService.validateAndSend(deletedBy, record, producer)
  }
}
