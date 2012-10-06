package actors

import scala.concurrent.util.duration._
import akka.actor.{ Actor, ActorRef, ActorLogging }
import play.api.libs.concurrent.execution.defaultContext
import models.github._

class GitHubNode extends Actor with ActorLogging {
  import HeadNode.NodeQuery
  import GitHubNode._

  def receive = {
    case NodeQuery(gitHubUser, gathererRef) => {
      log.debug("[GitHubNode] receiving new head query : " + gitHubUser)
      GitHubAPI.repositories("id").onComplete {
        case Right(repositories) => gathererRef ! GitHubNodeResult(repositories)
        case Left(e) => {
          log.error("[GitHubNode] Error while fetching repositories")
          gathererRef ! GitHubNodeError(e)
        }
      }
    }
  }
}

object GitHubNode {
  case class GitHubNodeError(e: Throwable)
  case class GitHubNodeResult(repositories: Set[GitHubRepository])
}
