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
import models.github._
import models.twitter._
import models.klout._
import models.popular._
import models.popular.PopularCoderJson
import models.CoderGuy
import utils.Config

object Application extends Controller {

  def index = Action { implicit request =>
    Logger.debug("[Application] Welcome !")
    Async {
      PopularCoder.top(10).map { coders =>
        PopularCoderJson.readPopularCoders.reads(JsArray(coders)).map { popular =>
          Ok(views.html.index(popular))
        }.recoverTotal { error =>
          InternalServerError(error.toString)
        }
      }
    }
  }

  def home = Action {
    Async {
      PopularCoder.top(10).map { coders =>
        PopularCoderJson.readPopularCoders.reads(JsArray(coders)).map { popular =>
          Ok(views.html.search() += views.html.popular(popular))
        }.recoverTotal { error =>
          InternalServerError(error.toString)
        }
      }
    }
  }

  def search(keywords: String) = Action {
    import GitHubAPI._
    Logger.debug("[Application] Pre-searching coder guy")
    //Ok(views.html.results(models.Mock.searchedGitHubUser()))
    Async {
      GitHubAPI.searchByFullname(keywords).map { gitHubUsers =>
        Ok(views.html.results(gitHubUsers))
      } recover {
        case e: Exception => {
          e.printStackTrace
          InternalServerError(e.getMessage)
        }
      }
    }
  }

  def profile(username: String) = Action {
    Logger.debug("[Application] Searching coder guy : " + username)
    implicit val timeout = Timeout(Config.overviewTimeout)
    //Ok(views.html.profile(models.Mock.coderGuy))
    Async {
      (for {
        user <- GitHubAPI.profile(username)
        if(user.isDefined)
        coderGuy <- (SupervisorNode.ref ? InitQuery(user.get)).mapTo[CoderGuy]
      } yield {
        Ok(views.html.profile(coderGuy))
      }) recover {
        case e: Exception => {
          e.printStackTrace
          InternalServerError(e.getMessage)
        }
      }
    }
  }

  def progress(username: String) = Action {
    Async {
      implicit val timeout = Timeout(20.seconds)
      (SupervisorNode.ref ? AskProgress(username)).mapTo[Enumerator[Double]].map { progress =>
        implicit val progressPulling = Comet.CometMessage[Double](_.toString)
        Ok.stream(progress &> EventSource())
          .withHeaders(CONTENT_TYPE -> "text/event-stream")
      } recover {
        case e: Exception => {
          e.printStackTrace
          InternalServerError(e.getMessage)
        }
      }
    }
  }
}
