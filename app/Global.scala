import play.api._
import play.api.mvc._
import play.api.mvc.Results._

object Global extends GlobalSettings {
  override def onHandlerNotFound(request: RequestHeader): Result = {
    println(request.uri)
    Redirect(controllers.routes.Application.index)
  }
}
