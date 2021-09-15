package model

import java.time.ZonedDateTime
import java.util.UUID

import play.api.libs.json.{Json, OWrites}

case class Question(id: UUID, title: String, details: Option[String],
                    tags: Seq[Tag], created: ZonedDateTime, authorId: UUID,
                    authorFullName: Option[String])

object Question {
  implicit val writes: OWrites[Question] = Json.writes[Question]
}
