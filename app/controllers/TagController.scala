package controllers

import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import security.UserAuthAction
import services.{ReadService, TagEventProducer}

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
  def createTag(): Action[AnyContent] = userAuthAction { implicit request =>
    createTagForm.bindFromRequest().fold(
      _ => BadRequest,
      data => {
        tagEventProducer.createTag(data.text, request.user.userId)
        Ok
      }
    )
  }

  def deleteTag(): Action[AnyContent] = userAuthAction { implicit request =>
    deleteTagForm.bindFromRequest().fold(
      _ => BadRequest,
      data => {
        tagEventProducer.deleteTag(data.id, request.user.userId)
        Ok
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
