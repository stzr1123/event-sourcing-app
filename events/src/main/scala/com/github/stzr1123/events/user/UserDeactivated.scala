package com.github.stzr1123.events.user

import com.github.stzr1123.events.EventData
import play.api.libs.json.{JsValue, Json, Reads}

import java.util.UUID

case class UserDeactivated(id: UUID) extends EventData {
  override def action: String = UserDeactivated.actionName
  override def json: JsValue = Json.writes[UserDeactivated].writes(this)
}

object UserDeactivated {
  val actionName: String = "user-deactivated"
  implicit val reads: Reads[UserDeactivated] = Json.reads[UserDeactivated]
}
