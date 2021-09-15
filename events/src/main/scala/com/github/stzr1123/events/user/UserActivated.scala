package com.github.stzr1123.events.user

import com.github.stzr1123.events.EventData
import play.api.libs.json.{JsValue, Json, Reads}

import java.util.UUID

case class UserActivated(id: UUID) extends EventData {
  override def action: String = UserActivated.actionName

  override def json: JsValue = Json.writes[UserActivated].writes(this)
}

object UserActivated {
  val actionName: String = "user-activated"
  implicit val reads: Reads[UserActivated] = Json.reads[UserActivated]
}
