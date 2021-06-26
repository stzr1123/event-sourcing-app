package controllers

import controllers.Assets.Asset
import model._
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.mvc._
import security.{UserAuthAction, UserAwareAction, UserAwareRequest}
import services.ClientBroadcastService


class MainController(components: ControllerComponents, assets: Assets,
                     clientBroadcastService: ClientBroadcastService,
                     userAuthAction: UserAuthAction,
                     userAwareAction: UserAwareAction)
  extends AbstractController(components) {
  import util.ThreadPools.CPU

  def index(): Action[AnyContent] = userAwareAction { request =>
    Ok(views.html.pages.react(buildNavData(request),
      WebPageData("Home")))
  }

  def error500(): Action[AnyContent] = Action {
    InternalServerError(views.html.errorPage())
  }

  private def buildNavData(request: UserAwareRequest[_]): NavigationData = {
    NavigationData(request.user, isLoggedIn = request.user.isDefined)
  }

  def versioned(path: String, file: Asset): Action[AnyContent] = assets.versioned(path, file)

  def serverEventStream() = userAwareAction { request =>
    val source = clientBroadcastService.createEventStream(request.user.map(_.userId))
    Ok.chunked(source.via(EventSource.flow)).as(ContentTypes.EVENT_STREAM)
  }
}
