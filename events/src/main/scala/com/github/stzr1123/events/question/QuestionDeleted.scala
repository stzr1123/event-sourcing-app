package com.github.stzr1123.events.question

import com.github.stzr1123.events.EventData
import play.api.libs.json.{JsValue, Json, Reads}

import java.util.UUID

case class QuestionDeleted(questionId: UUID, deletedBy: UUID) extends EventData {
  override def action: String = QuestionDeleted.actionName
  override def json: JsValue = Json.writes[QuestionDeleted].writes(this)
}

object QuestionDeleted {
  val actionName = "question-deleted"
  implicit val reads: Reads[QuestionDeleted] = Json.reads[QuestionDeleted]
}
