package services

import com.github.stzr1123.events.LogRecord
import dao.{InMemoryReadDao, LogDao}
import model.ApplicationState
import play.api.Logger

import scala.concurrent.Future

class ReadService(readDao: InMemoryReadDao, logDao: LogDao) {
  private val log = Logger(this.getClass)

  import util.ThreadPools.CPU
  def init(): Future[Unit] = {
    logDao.getLogRecords.flatMap { events =>
      readDao.processEvents(events)
    }
  }
  def getState: Future[ApplicationState] = {
    readDao.getState
  }
  def adjustState(event: LogRecord): Future[Unit] = {
    readDao.processEvent(event)
  }
}
