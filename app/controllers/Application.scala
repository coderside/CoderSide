package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent._
import play.api.libs.concurrent.execution.defaultContext
import play.api.libs.json._
import play.api.libs.json.Json._
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration._
import actors.{ SupervisorNode, GathererNode }
import actors.Messages.InitQuery
import models.github.{ GitHubAPI, GitHubUser }
import models.CoderGuy

object Application extends Controller {

  def index = Action {
    Logger.debug("[Application] Welcome !")
    Ok(views.html.index())
  }

  def search(keywords: String) = Action {
    Logger.debug("[Application] Searching coder guy with " + keywords.mkString(" / "))
    implicit val timeout = Timeout(20 second)
    Async {
      GitHubAPI.searchByFullname(keywords).flatMap { gitHubUsers =>
        if(gitHubUsers.size > 0) {
          (SupervisorNode.ref ? InitQuery(gitHubUsers.head)).mapTo[CoderGuy].asPromise.map { coderGuy =>
            Ok(toJson(coderGuy))
          }.recover {
            case e: Exception => InternalServerError(e.getMessage)
          }
        } else Promise.pure(BadRequest)
      }
    }
  }
}
