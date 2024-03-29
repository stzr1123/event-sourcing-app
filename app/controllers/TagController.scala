package controllers

import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import security.UserAuthAction
import services.{ReadService, TagEventProducer}

import scala.concurrent.Future

class TagController(components: ControllerComponents,
                    tagEventProducer: TagEventProducer,
                    userAuthAction: UserAuthAction,
                    readService: ReadService) extends AbstractController(components) {
  import play.api.data.Form
  import play.api.data.Forms._

  private val createTagForm = Form {
    mapping(
      "text" -> nonEmptyText
    )(CreateTagData.apply)(CreateTagData.unapply)
  }

  private val deleteTagForm = Form {
    mapping(
      "id" -> uuid
    )(DeleteTagData.apply)(DeleteTagData.unapply)
  }

  import util.ThreadPools.CPU
  def createTag(): Action[AnyContent] = userAuthAction.async { implicit request =>
    createTagForm.bindFromRequest().fold(
      _ => Future.successful(BadRequest),
      data => {
        tagEventProducer.createTag(data.text, request.user.userId).map {
          case Some(_) => InternalServerError
          case None => Ok
        }
      }
    )
  }

  def deleteTag(): Action[AnyContent] = userAuthAction.async { implicit request =>
    deleteTagForm.bindFromRequest().fold(
      _ => Future.successful(BadRequest),
      data => {
        tagEventProducer.deleteTag(data.id, request.user.userId).map {
          case Some(_) => InternalServerError
          case None => Ok
        }
      }
    )
  }

  def getTags: Action[AnyContent] = Action.async { implicit request =>
    readService.getState.map { state =>
      Ok(Json.toJson(state.tags))
    }
  }

  import java.util.UUID
  case class CreateTagData(text: String)
  case class DeleteTagData(id: UUID)
}
