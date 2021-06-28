package services

import com.github.stzr1123.events.LogRecord
import messaging.{IMessageConsumer, IMessageProcessingRegistry}
import model.ServerSentMessage
import play.api.Logger

import scala.concurrent.Future

class TagEventConsumer(readService: ReadService,
                       clientBroadcastService: ClientBroadcastService,
                       registry: IMessageProcessingRegistry) extends IMessageConsumer {
  private val log = Logger(this.getClass)
  registry.registerConsumer("read.tags", this)

  import util.ThreadPools.CPU

  def adjustReadState(logRecord: LogRecord): Future[Unit] = {
    readService.adjustState(logRecord).flatMap{ _ =>
      readService.getState.map(_.tags)
    }.map { tags =>
      val update = ServerSentMessage.create("tags", tags)
      clientBroadcastService.broadcastUpdate(update.json)
    }
  }

  override def messageReceived(event: Array[Byte]): Unit = {
    adjustReadState(LogRecord.decode(event)).recover { case th =>
    log.error("Error occurred while adjusting read state")
    }
  }
}
