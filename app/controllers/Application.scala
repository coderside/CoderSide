package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent._
import play.api.libs.concurrent.execution.defaultContext
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration._
import actors.{ SupervisorNode, GathererNode }
import actors.Messages.InitQuery
import models.github.{ GitHubAPI, GitHubUser }
import models.CoderGuy

object Application extends Controller {

  def index = Action {
    Logger.debug("[Application] Sending query ...")
    implicit val timeout = Timeout(20 second)
    Async {
      GitHubAPI.searchByFullname("Sébastien Renault").flatMap { gitHubUsers =>
        (SupervisorNode.ref ? InitQuery(gitHubUsers.head)).mapTo[CoderGuy].asPromise.map { coderGuy =>
          Ok(coderGuy.toString)
        }.recover {
          case e: Exception => InternalServerError(e.getMessage)
        }
      }
      Promise.pure(Ok)
    }
  }

  def search(keywords: List[String]) = Action {
    Ok
  }
}
