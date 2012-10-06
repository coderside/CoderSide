package actors

import akka.actor.{ Actor, ActorRef, ActorLogging }
import scala.concurrent.util.duration._

class GathererNode(awaited: Int, client: ActorRef) extends Actor with ActorLogging {
  import GitHubNode.{ GitHubNodeResult, GitHubNodeError }
  import GathererNode.QueryResult

  def receive = {
    case GitHubNodeResult(repositories) => {
      log.debug("[GathererNode] receiving github repositories: " + repositories)
      client ! QueryResult(repositories)
    }
    case GitHubNodeError(e) => {
      log.info("[GathererNode] Following error well received " + e.getMessage)
      client ! e
    }
  }

  override def preStart() = {
    log.debug("[GathererNode] Starting with " + awaited)
  }

  override def postStop() = {
    log.debug("[GathererNode] after stopping...")
  }
}

object GathererNode {
  import models.github.GitHubRepository
  case class QueryResult(repositories: Set[GitHubRepository])
}
