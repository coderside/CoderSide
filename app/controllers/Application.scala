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
import models.{ CoderGuy, PopularCoder }
import utils.Config

object Application extends Controller {

  def index = Action { implicit request =>
    Logger.debug("[Application] Welcome !")
    import models.PopularCoder.json._
    Async {
      PopularCoder.top(10).map { coders =>
        Ok(views.html.index(coders.flatMap(_.asOpt[PopularCoder])))
      }
    }
  }

  def home = Action {
    import models.PopularCoder.json._
    Async {
      PopularCoder.top(10).map { coders =>
        Ok(views.html.search() += views.html.popular(coders.flatMap(_.asOpt[PopularCoder])))
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

  def profile(username: String, fullname: String) = Action {
    Logger.debug("[Application] Searching coder guy")
    val name = Option(fullname) filter (!_.trim.isEmpty)
    val gitHubUser = GitHubSearchedUser(username, name)
    implicit val timeout = Timeout(Config.overviewTimeout)
    //Ok(views.html.profile(models.Mock.coderGuy))
    Async {
      (SupervisorNode.ref ? InitQuery(gitHubUser)).mapTo[CoderGuy].map { coderGuy =>
        Ok(views.html.profile(coderGuy))
      } recover {
        case e: Exception => {
          e.printStackTrace
          InternalServerError(e.getMessage)
        }
      }
    }
  }

  def progress(username: String, fullname: String) = Action {
    val name = Option(fullname) filter (!_.trim.isEmpty)
    val gitHubUser = GitHubSearchedUser(username, name)
    Async {
      implicit val timeout = Timeout(20.seconds)
      (SupervisorNode.ref ? AskProgress(gitHubUser)).mapTo[Enumerator[Double]].map { progress =>
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
