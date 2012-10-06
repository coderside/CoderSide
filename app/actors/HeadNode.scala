package actors

import akka.actor.{ Actor, ActorRef, Props, ActorLogging }
import scala.concurrent.util.duration._

class HeadNode() extends Actor with ActorLogging {
  import HeadNode.{ NodeQuery, HeadQuery }

  def gathererNode(awaited: Int, client: ActorRef) = context.actorOf(Props(new GathererNode(awaited, client)))
  lazy val  gitHubNode = context.actorOf(Props[GitHubNode])

  def receive = {
    case HeadQuery(gitHubUsers, client) => {
      log.debug("[HeadNode] receiving new init query : " + gitHubUsers)
      val gathererRef = gathererNode(gitHubUsers.size, client)
      gitHubUsers.foreach { gitHubUser =>
        gitHubNode ! NodeQuery(gitHubUser, gathererRef)
      }
    }
  }

  override def preStart() = {
    log.debug("[HeadNode] before starting...")
  }

  override def postStop() = {
    log.debug("[HeadNode] after stopping...")
  }
}

object HeadNode {
  import models.github.GitHubUser

  case class HeadQuery(gitHubUsers: Set[GitHubUser], client: ActorRef)
  case class NodeQuery(gitHubUser: GitHubUser, gatherer: ActorRef)
}
