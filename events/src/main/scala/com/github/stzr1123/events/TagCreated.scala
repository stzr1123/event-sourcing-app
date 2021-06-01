package com.github.stzr1123.events

import play.api.libs.json.{JsValue, Json, Reads}

import java.util.UUID

case class TagCreated(id: UUID, text: String, createdBy: UUID) extends EventData {
  override def action = TagCreated.actionName
  override def json: JsValue = Json.writes[TagCreated].writes(this)
}

object TagCreated {
  val actionName = "tag-created"
  implicit val reads: Reads[TagCreated] = Json.reads[TagCreated]
}


