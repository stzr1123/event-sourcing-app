package messaging

trait IMessageProducer {
  // NOTE: why Unit and not Future[Unit]?
  def send(bytes: Array[Byte]): Unit
}
