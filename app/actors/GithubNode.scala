package actors

import scala.concurrent.util.duration._
import akka.actor.{ Actor, ActorRef, ActorLogging }
import play.api.libs.concurrent.execution.defaultContext
import models.github._
import Messages._

class GitHubNode extends Actor with ActorLogging {
  def receive = {
    case NodeQuery(gitHubUser, gathererRef) => {
      log.debug("[GitHubNode] receiving new head query : " + gitHubUser)
      GitHubAPI.repositories("id").onComplete {
        case Right(repositories) => gathererRef ! GitHubResult(repositories)
        case Left(e) => {
          log.error("[GitHubNode] Error while fetching repositories")
          gathererRef ! ErrorQuery(e)
        }
      }
    }
  }
}
