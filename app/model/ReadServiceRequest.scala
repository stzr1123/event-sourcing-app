package model

import com.github.stzr1123.events.LogRecord

sealed trait ReadServiceRequest

case class GetStateRequest(callback: ApplicationState => Unit)
  extends ReadServiceRequest

case class UpdateStateRequest(event: LogRecord)
  extends ReadServiceRequest
