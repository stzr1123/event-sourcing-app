package model

import play.api.libs.json.Json

import java.util.UUID

case class Tag(id: UUID, text: String)

object Tag {
  implicit val writes = Json.writes[Tag]
}
