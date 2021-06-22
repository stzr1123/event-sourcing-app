package com.github.stzr1123.events

import play.api.libs.json.{JsValue, Json, OFormat}

import java.time.ZonedDateTime
import java.util.UUID

case class LogRecord(id: UUID, action: String,
                     data: JsValue, timestamp: ZonedDateTime) {
  def encode: Array[Byte] = {
    Json.toJson(this)(LogRecord.format).toString.getBytes
  }
}

object LogRecord {
  val format: OFormat[LogRecord] = Json.format[LogRecord]
  def decode(bytes: Array[Byte]): LogRecord = {
    Json.parse(bytes).as[LogRecord](format)
  }

  def fromEvent(eventData: EventData): LogRecord = {
    LogRecord(UUID.randomUUID(), eventData.action, eventData.json, ZonedDateTime.now())
  }
}
