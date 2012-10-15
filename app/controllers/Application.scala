package controllers

import scala.concurrent.util.duration._
import play.api._
import play.api.mvc._
import play.api.libs.concurrent._
import play.api.libs.concurrent.execution.defaultContext
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

object Application extends Controller {

  def index = Action {
    Logger.debug("[Application] Welcome !")
    Ok(views.html.index())
  }

  def progress(keywords: String) = Action {
    Async {
      implicit val timeout = Timeout(20.seconds)
      (SupervisorNode.ref ? AskProgress(keywords)).mapTo[Enumerator[Float]].map { progress =>
        implicit val progressPulling = Comet.CometMessage[Float](_.toString)
        Ok.stream(progress &> EventSource())
          .withHeaders(CONTENT_TYPE -> "text/event-stream")
      }
    }
  }

  def search(keywords: String) = Action {
    Logger.debug("[Application] Searching coder guy with " + keywords)
    implicit val timeout = Timeout(20.seconds)
    Async {
      GitHubAPI.searchByFullname(keywords).flatMap { gitHubUsers =>
        if(gitHubUsers.size > 0) {
          (SupervisorNode.ref ? InitQuery(keywords, gitHubUsers.head)).mapTo[CoderGuy].map { coderGuy =>
            Ok(toJson(coderGuy))
          }.recover {
            case e: Exception => InternalServerError(e.getMessage)
          }
        } else Promise.pure(NotFound)
      }
    }
  }
}
