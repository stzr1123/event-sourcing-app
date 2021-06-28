package model

import play.api.libs.json.{JsValue, Json, OWrites, Writes}

case class ServerSentMessage(updateType: String, updateData: JsValue) {
  def json: JsValue = Json.toJson(this)(ServerSentMessage.writes)
}

object ServerSentMessage {
  val writes: OWrites[ServerSentMessage] = Json.writes[ServerSentMessage]
  def create[T](updateType: String, updateData: T)(implicit encoder: Writes[T]): ServerSentMessage =
    ServerSentMessage(updateType, encoder.writes(updateData))
}
