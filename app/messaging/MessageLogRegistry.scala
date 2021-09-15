package messaging

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer}
import play.api.{Configuration, Logger}

class MessageLogRegistry(configuration: Configuration,
                         actorSystem: ActorSystem)
                        (implicit val mat: Materializer)
  extends IMessageProcessingRegistry {
  private val log = Logger(this.getClass)
  private val bootstrapServers = configuration.get[String]("kafka.bootstrap.servers")
  private val offsetReset = configuration.get[String]("kafka.auto.offset.reset")

  case class ConsumerParams(groupName: String, topics: Set[String])

  private val AllTopics = Set("tags", "users")

  private def parseConsumerParams(queue: String): ConsumerParams = {
    val parts = queue.split("\\.")
    val topics = if (parts(1) == "*") AllTopics else Set(parts(1))
    ConsumerParams(parts(0), topics)
  }

  private def consumerSettings(groupName: String): ConsumerSettings[Array[Byte], Array[Byte]] = ConsumerSettings(actorSystem,
    new ByteArrayDeserializer,
    new ByteArrayDeserializer)
    .withBootstrapServers(bootstrapServers)
    .withGroupId(groupName)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset)

  /*
    In the case of akka-stream-kafka no shutdown operation is needed.
   */
  override def shutdown(): Unit = ()

  override def createProducer(topic: String): IMessageProducer = {
    val producerSettings = ProducerSettings(actorSystem,
      new ByteArraySerializer, new ByteArraySerializer)
      .withBootstrapServers(bootstrapServers)
    val producer = producerSettings.createKafkaProducer()
    // since IMessageProducer is a single method interface (i.e. "functional interface")
    // we can simply return a lambda function
    (bytes: Array[Byte]) => producer.send(new ProducerRecord(topic, bytes))
  }

  override def registerConsumer(queue: String, consumer: IMessageConsumer): Unit = {
    val ConsumerParams(groupName, topics) = parseConsumerParams(queue)
    Consumer.atMostOnceSource(consumerSettings(groupName),
      Subscriptions.topics(topics)).map { msg =>
      consumer.messageReceived(msg.value())
      msg
    }.toMat(Sink.ignore)(DrainingControl.apply).run()
  }
}
