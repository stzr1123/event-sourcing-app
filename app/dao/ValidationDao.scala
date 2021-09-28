package dao

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.github.stzr1123.events.question.{QuestionCreated, QuestionDeleted}
import com.github.stzr1123.events.user.{UserActivated, UserDeactivated}
import com.github.stzr1123.events.{LogRecord, TagCreated, TagDeleted}
import scalikejdbc.interpolation.SQLSyntax
import scalikejdbc.{NamedDB, ParameterBinderFactory, scalikejdbcSQLInterpolationImplicitDef}

import java.util.UUID
import scala.concurrent.Future
import scala.util.{Failure, Success}

class ValidationDao(implicit mat: Materializer) {
  import util.ThreadPools.CPU
  def validateSingle(event: LogRecord): Future[Option[String]] = {
    processSingleEvent(event, skipValidation = false)
  }

  private def processSingleEvent(event: LogRecord, skipValidation: Boolean): Future[Option[String]] = {
    event.action match {
      // TODO: add the other events here
      case UserActivated.actionName =>
        val decoded = event.data.as[UserActivated]
        validateAndUpdate(skipValidation) {
          validateUserActivated(decoded.id)
        } { updateUserActivated(decoded.id) }
      case UserDeactivated.actionName =>
        val decoded = event.data.as[UserDeactivated]
        validateAndUpdate(skipValidation) {
          validateUserDeactivated(decoded.id)
        } { updateUserDeactivated(decoded.id) }
      case TagCreated.actionName =>
        val decoded = event.data.as[TagCreated]
        validateAndUpdate(skipValidation) {
          validateTagCreated(decoded.text, decoded.createdBy)
        } { updateTagCreated(decoded.id, decoded.text) }
      case TagDeleted.actionName =>
        val decoded = event.data.as[TagDeleted]
        validateAndUpdate(skipValidation) {
          validateTagDeleted(decoded.id, decoded.deleteBy)
        } { updateTagDeleted(decoded.id) }
      case QuestionCreated.actionName =>
        val decoded = event.data.as[QuestionCreated]
        validateAndUpdate(skipValidation) {
          validateQuestionCreated(decoded.questionId, decoded.createdBy, decoded.tags)
        } { updateQuestionCreated(decoded.questionId, decoded.createdBy, decoded.tags) }
      case QuestionDeleted.actionName =>
        val decoded = event.data.as[QuestionDeleted]
        validateAndUpdate(skipValidation) {
          validateQuestionDeleted(decoded.questionId, decoded.deletedBy)
        } { updateQuestionDeleted(decoded.questionId) }
      case _ => Future.successful(Some("Unknown event"))
    }
  }

  private def processEvents(events: Seq[LogRecord], skipValidation: Boolean): Future[Option[String]] = {
    Source.apply(events).foldAsync(Option.empty[String]) {
      (previousResult, nextEvent) =>
        previousResult match {
          case None => processSingleEvent(nextEvent, skipValidation)
          case _  => Future.successful(previousResult)
        }
    }.runWith(Sink.last)
  }

  private def resetState(fromScratch: Boolean): Future[Option[String]] = {
    // NOTE: kind of weird to have the fromScratch boolean only
    //       to do nothing
    if (!fromScratch) Future.successful(None)
    else invokeUpdate {
      NamedDB(Symbol("validation")).localTx { implicit session =>
        sql"delete from tags where 1 > 0".update().apply()
        sql"delete from active_users where 1 > 0".update().apply()
        sql"delete from question_user where 1 > 0".update().apply()
        sql"delete from tag_question where 1 > 0".update().apply()
      }
    }
  }

  /**
   * Applies multiple events after an optional state reset.
   * @param events
   * @param fromScratch
   * @return
   */
  def refreshState(events: Seq[LogRecord], fromScratch: Boolean): Future[Option[String]] = {
    resetState(fromScratch).flatMap {
      case Some(value) => Future.successful(Some(value))
      case None => processEvents(events, skipValidation = true)
    }
  }

  import util.ThreadPools.IO
  private def isActivated(userId: UUID): Future[Boolean] = {
    Future {
      NamedDB(Symbol("validation")).readOnly { implicit session =>
        sql"select user_id from active_users where user_id = $userId".
          map(_.string("user_id")).headOption().apply().isDefined
      }
    }
  }

  private def invokeUpdate(block: => Any): Future[Option[String]] = {
    val result = Future { block }
    result.transform {
      case Success(_) => Success(None)
      case Failure(th) => Success(Some("Validation state exception"))
    }
  }

  private def validateAndUpdate(skipValidation: Boolean)
                               (validateBlock: => Future[Option[String]])
                               (updateBlock: => Future[Option[String]]): Future[Option[String]] = {
    if (skipValidation) {
      updateBlock
    } else {
      val validationResult = validateBlock
      validationResult.transformWith {
        case Success(None) => updateBlock
        case _ => validationResult
      }
    }
  }

  private def validateUser(userId: UUID)(block: => Future[Option[String]]): Future[Option[String]] = {
    isActivated(userId).transformWith {
      case Success(false) =>
        Future.successful(Some("The user is not activated!"))
      case Failure(_) =>
        Future.successful(Some("Validation state exception!"))
      case Success(true) => block
    }
  }

  private def validateUserActivated(userId: UUID): Future[Option[String]] = {
    isActivated(userId).transform {
      case Success(true) =>
        Success(Some("The user is already activated!"))
      case Failure(_) =>
        // NOTE: We lose error information downstream
        Success(Some("Validation state exception"))
      case Success(false) => Success(None)
    }
  }

  private def updateUserActivated(userId: UUID): Future[Option[String]] = {
    invokeUpdate {
      NamedDB(Symbol("validation")).localTx { implicit session =>
        sql"insert into active_users(user_id) values($userId)".
          update().apply()
      }
    }
  }

  private def validateUserDeactivated(userId: UUID): Future[Option[String]] = {
    isActivated(userId).transform {
      case Success(true) => Success(None)
      case Failure(_) => Success(Some("Validation state exception"))
      case Success(false) => Success(Some("The user is already deactivated!"))
    }
  }

  private def updateUserDeactivated(userId: UUID): Future[Option[String]] = {
    invokeUpdate {
      NamedDB(Symbol("validation")).localTx { implicit session =>
        sql"delete from active_users where user_id = $userId".update().apply()
      }
    }
  }

  private def validateTagCreated(tagText: String, userId: UUID): Future[Option[String]] = {
      validateUser(userId) {
        val maybeExistingF = Future {
          NamedDB(Symbol("validation")).readOnly { implicit session =>
            // TODO: This query does not scale, it's a linear scan on text.
            sql"select tag_id from tags where tag_text = $tagText".
              map(_.string("tag_id")).headOption().apply()
          }
        }
        maybeExistingF.transform {
          case Success(Some(_)) => Success(Some("The tag already exists!"))
          case Success(None) => Success(None)
          case _ => Success(Some("Validation state exception!"))
        }
      }
    }

  private def updateTagCreated(tagId: UUID, tagText: String): Future[Option[String]] = {
    invokeUpdate {
      NamedDB(Symbol("validation")).localTx { implicit session =>
        sql"insert into tags(tag_id, tag_text) values($tagId, $tagText)".
          update().apply()
      }
    }
  }

  private def validateTagDeleted(tagId: UUID, userId: UUID): Future[Option[String]] = {
    validateUser(userId) {
      Future {
        val maybeExistingTag = NamedDB(Symbol("validation")).readOnly { implicit session =>
          sql"select tag_id from tags where tag_id = ${tagId}".
            map(_.string("tag_id")).headOption().apply()
        }
        val maybeDependentQuestions = NamedDB(Symbol("validation")).readOnly { implicit session =>
          sql"select question_id from tag_question where tag_id = ${tagId}".
            map(_.string("question_id")).list().apply()
        }
        (maybeExistingTag, maybeDependentQuestions) match {
          case (None, _) => Some("This tag doesn't exist!")
          case (_, head :: tail) => Some("There are questions that depend on this tag!")
          case (_, Nil) => None
        }
      }
    }
  }

  private def updateTagDeleted(tagId: UUID): Future[Option[String]] = {
    invokeUpdate {
      NamedDB(Symbol("validation")).localTx { implicit session =>
        sql"delete from tags where tag_id = ${tagId}".update().apply()
      }
    }
  }

  private def validateQuestionCreated(questionId: UUID, userId: UUID, tags: Seq[UUID]): Future[Option[String]] = {
    validateUser(userId) {
      val existingTagsF = Future {
        NamedDB(Symbol("validation")).localTx { implicit session =>
          implicit val binderFactory: ParameterBinderFactory[UUID] = ParameterBinderFactory {
            value => (stmt, idx) => stmt.setObject(idx, value)
          }
          val tagIdsSql = SQLSyntax.in(sqls"tag_id", tags)
          sql"select * from tags where $tagIdsSql".map(_.string("tag_id")).list().apply().length
        }
      }
      existingTagsF.transform {
        case Failure(th) => Success(Some("Validation state exception"))
        case Success(num) if num == tags.length => Success(None)
        case _ => Success(Some("Some tags referenced by the question do no exist!"))
      }
    }
  }

  private def updateQuestionCreated(questionId: UUID, userId: UUID, tags: Seq[UUID]): Future[Option[String]] = {
    invokeUpdate {
      NamedDB(Symbol("validation")).localTx { implicit session =>
        sql"insert into question_user(question_id, user_id) values(${questionId}, ${userId})".update().apply()
        tags.foreach { tagId =>
          sql"insert into tag_question(tag_id, question_id) values(${tagId}, ${questionId})".update().apply()
        }
      }
    }
  }

  private def validateQuestionDeleted(questionId: UUID, userId: UUID): Future[Option[String]] = {
    validateUser(userId) {
      val maybeQuestionOwnerF = Future {
        NamedDB(Symbol("validation")).readOnly { implicit session =>
            sql"select user_id from question_user where question_id = $questionId".
              map(_.string("user_id")).headOption().apply()
        }
      }
      maybeQuestionOwnerF.transform {
        case Success(None) => Success(Some("The question doesn't exist!"))
        case Success(Some(questionOwner)) =>
          if (questionOwner != userId.toString) {
            Success(Some("This user has no rights to delete this question!"))
          } else {
            Success(None)
          }
        case _ => Success(Some("Validation state exception!"))
      }
    }
  }

  private def updateQuestionDeleted(id: UUID): Future[Option[String]] = {
    invokeUpdate {
      NamedDB(Symbol("validation")).localTx { implicit session =>
        sql"delete from question_user where question_id = $id"
      }
    }
  }

}
