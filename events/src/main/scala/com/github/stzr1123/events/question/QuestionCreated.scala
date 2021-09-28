package com.github.stzr1123.events.question

import com.github.stzr1123.events.EventData
import play.api.libs.json.{JsValue, Json, Reads}

import java.time.ZonedDateTime
import java.util.UUID

case class QuestionCreated(title: String, details: Option[String],
                           tags: Seq[UUID], questionId: UUID, createdBy: UUID,
                           created: ZonedDateTime) extends EventData {
  override def action: String = QuestionCreated.actionName

  override def json: JsValue = Json.writes[QuestionCreated].writes(this)
}

object QuestionCreated {
  val actionName = "question-created"
  implicit val reads: Reads[QuestionCreated] = Json.reads[QuestionCreated]
}
