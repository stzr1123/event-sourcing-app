package com.github.stzr1123.events

import play.api.libs.json.JsValue

trait EventData {
  def action: String
  def json: JsValue
}
