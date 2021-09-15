package model

import play.api.libs.json.{Json, OWrites}

import java.util.UUID

case class Tag(id: UUID, text: String)

object Tag {
  implicit val writes: OWrites[Tag] = Json.writes[Tag]
}
