package services

import org.neo4j.driver.async.AsyncSession
import org.neo4j.driver.summary.SummaryCounters
import org.neo4j.driver.{AuthTokens, GraphDatabase, Record}
import play.api.Configuration

import java.util.concurrent.CompletionStage
import scala.concurrent.Future
import scala.util.Using

case class Neo4JUpdate(queries: Seq[Neo4JQuery])

case class Neo4JQuery(query: String, params: Map[String, AnyRef]) {
  def paramsAsJava: java.util.Map[String, AnyRef] = {
    import scala.jdk.CollectionConverters._
    params.asJava
  }
}

object Neo4JQuery {
  def simple(query: String): Neo4JQuery =
    Neo4JQuery(query, Map.empty[String, AnyRef])
}

class Neo4JQueryExecutor(configuration: Configuration) {
  private val config = configuration.get[Configuration]("neo4j")
  private val driver = GraphDatabase.driver(
    config.get[String]("url"),
    AuthTokens.basic(
      config.get[String]("username"),
      config.get[String]("password")
    )
  )
  import util.ThreadPools.IO
  def executeSequentially(update: Neo4JUpdate): Future[Unit] = {
    executeBatch(update.queries)
  }

  import scala.jdk.CollectionConverters._
  import scala.jdk.FutureConverters._
  def executeQuery(query: Neo4JQuery): Future[Seq[Record]] = {
    doWithSession { session =>
      val result = session.runAsync(query.query, query.paramsAsJava)
      result.thenCompose(_.listAsync())
    }.asScala.map(_.asScala.toSeq)
  }

  private def doWithSession[A](block: AsyncSession => CompletionStage[A]): CompletionStage[A] = {
    val session = driver.asyncSession()
    block(session).whenCompleteAsync((cursor, th) => {
      session.closeAsync()
    })
  }

  def executeUpdate(update: Neo4JQuery): Future[SummaryCounters] = {
    doWithSession { session =>
      session.runAsync(update.query, update.paramsAsJava).
        thenCompose(_.consumeAsync().thenApply(_.counters()))
    }.asScala
  }

  def executeBatch(updates: Seq[Neo4JQuery]): Future[Unit] = Future {
    // NOTE: checkout the usage of Using for managing the resource driver and session
    Using.resource(driver.session()) { session =>
      Using.resource(session.beginTransaction()) { transaction =>
        updates.foreach { update =>
          transaction.run(update.query, update.paramsAsJava)
        }
        transaction.commit()
      }
    }
  }
}
