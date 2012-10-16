package actors

import scala.concurrent.util.duration._
import scala.util.{ Success, Failure }
import akka.actor.{ Actor, ActorRef, ActorLogging }
import play.api.libs.concurrent.execution.defaultContext
import models.github._
import Messages._

class GitHubNode extends Actor with ActorLogging {
  def receive = {
    case NodeQuery(gitHubUser, gathererRef) => {
      log.debug("[GitHubNode] receiving new head query")
      GitHubAPI.repositories(gitHubUser.username).onComplete {
        case Success(repositories) => gathererRef ! GitHubResult(repositories)
        case Failure(e) => {
          log.error("[GitHubNode] Error while fetching repositories")
          gathererRef ! ErrorQuery(e)
        }
      }
    }
  }
}
