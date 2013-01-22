package controllers

import scala.concurrent.duration._
import scala.concurrent.future
import play.api._
import play.api.mvc._
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.{ Comet, EventSource }
import play.api.libs.iteratee._
import akka.pattern.ask
import akka.util.Timeout
import actors.{ SupervisorNode, GathererNode }
import actors.Messages.{ InitQuery, AskProgress }
import models.github.{ GitHubAPI, GitHubUser }
import models.CoderGuy
import utils.Config

object Application extends Controller {

  def index = Action { implicit request =>
    Logger.debug("[Application] Welcome !")
    Ok(views.html.index())
  }

  def home = Action {
    Ok(views.html.search())
  }

  def search(keywords: String) = Action {
    import GitHubAPI._
    Logger.debug("[Application] Pre-searching coder guy")
    Async {
      GitHubAPI.searchByFullname(keywords).map { gitHubUsers =>
        Ok(views.html.results(List(GitHubUser("srenault", Some("RENAULT"), Some("scala"), None))))
      } recover {
        case e: Exception => InternalServerError(e.getMessage)
      }
    }
  }

  def profil(username: String, fullname: String, language: String) = Action {
    Logger.debug("[Application] Searching coder guy")
    val name = Option(fullname) filter (!_.trim.isEmpty)
    val lang = Option(language) filter (!_.trim.isEmpty) orElse Some("n/a")
    val gitHubUser = GitHubUser(username, name, lang)
    implicit val timeout = Timeout(Config.overviewTimeout)
    Async {
      future(Ok(views.html.profil(CoderGuy(Some(GitHubUser("srenault", Some("RENAULT"), None, None)), None, None, None))))
      // (SupervisorNode.ref ? InitQuery(gitHubUser)).mapTo[CoderGuy].map { coderGuy =>
      //   Ok(views.html.profil(coderGuy))
      // } recover {
      //   case e: Exception => InternalServerError(e.getMessage)
      // }
    }
  }

  def progress(username: String, fullname: String, language: String) = Action {
    val name = Option(fullname) filter (!_.trim.isEmpty)
    val lang = Option(language) filter (!_.trim.isEmpty) orElse Some("n/a")
    val gitHubUser = GitHubUser(username, name, lang)
    Async {
      implicit val timeout = Timeout(20.seconds)
      (SupervisorNode.ref ? AskProgress(gitHubUser)).mapTo[Enumerator[Float]].map { progress =>
        implicit val progressPulling = Comet.CometMessage[Float](_.toString)
        Ok.stream(progress &> EventSource())
          .withHeaders(CONTENT_TYPE -> "text/event-stream")
      } recover {
        case e: Exception => InternalServerError(e.getMessage)
      }
    }
  }
}
