package com.github.stzr1123.events

import play.api.libs.json.{JsValue, Json, Reads}

import java.util.UUID

case class TagDeleted(id: UUID, deleteBy: UUID) extends EventData {
  override def action: String = TagDeleted.actionName
  override def json: JsValue = Json.writes[TagDeleted].writes(this)
}

object TagDeleted {
  val actionName = "tag-deleted"
  implicit val reads: Reads[TagDeleted] = Json.reads[TagDeleted]
}
