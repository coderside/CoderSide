package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent._
import play.api.libs.concurrent.execution.defaultContext
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration._
import actors.{ SupervisorNode, GathererNode }
import models.github.GitHubUser

object Application extends Controller {

  def index = Action {
    import SupervisorNode.InitQuery
    import GathererNode.QueryResult

    Logger.debug("[Application] Sending query ...")
    implicit val timeout = Timeout(20 second)
    Async {
      (SupervisorNode.ref ? InitQuery(Set(GitHubUser()))).mapTo[QueryResult].asPromise.map { result =>
        Ok(result.toString)
      }
    }
  }

  def search(keywords: List[String]) = Action {
    Ok
  }
}
