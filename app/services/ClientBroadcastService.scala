package services

import akka.Done
import akka.stream.scaladsl.Source
import io.reactivex.rxjava3.processors.PublishProcessor
import org.reactivestreams.Subscriber
import play.api.libs.json.JsValue

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.Future

class ClientBroadcastService {
  import util.ThreadPools.CPU

  private val connectedClients = new ConcurrentHashMap[UUID, ConnectedClient]()

  def broadcastUpdate(data: JsValue): Future[Unit] = Future {
    connectedClients.values().forEach { client =>
      client.out.onNext(data)
    }
  }

  private def addConnectedClient(clientId: UUID, connectedClient: ConnectedClient): Unit =
    connectedClients.put(clientId, connectedClient)

  private def removeDisconnectedClient(clientId: UUID): Unit =
    connectedClients.remove(clientId)

  def createEventStream(userId: Option[UUID]): Source[JsValue, Future[Done]] = {
    val publisher = PublishProcessor.create[JsValue]()
    val clientId = UUID.randomUUID()
    val toClient: Source[JsValue, Future[Done]] = Source.fromPublisher(publisher)
      .watchTermination() { (_, done) =>
        done.andThen { case _ =>
        removeDisconnectedClient(clientId)
        }
      }
    val client = ConnectedClient(userId, publisher)
    addConnectedClient(clientId, client)
    toClient
  }


  case class ConnectedClient(userId: Option[UUID], out: Subscriber[JsValue])
}
