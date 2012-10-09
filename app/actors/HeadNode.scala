package actors

import akka.actor.{ Actor, ActorRef, Props, ActorLogging }
import scala.concurrent.util.duration._
import Messages._

class HeadNode() extends Actor with ActorLogging {

  def gathererNode(client: ActorRef) = context.actorOf(Props(new GathererNode(client)))

  lazy val gitHubNode = context.actorOf(Props[GitHubNode])
  lazy val linkedInNode = context.actorOf(Props[LinkedInNode])
  lazy val twitterNode = context.actorOf(Props[TwitterNode])
  lazy val kloutNode = context.actorOf(Props[KloutNode])

  def receive = {
    case HeadQuery(gitHubUser, client) => {
      log.debug("[HeadNode] receiving new init query : " + gitHubUser)
      val gathererRef = gathererNode(client)
      gitHubNode ! NodeQuery(gitHubUser, gathererRef)
      linkedInNode ! NodeQuery(gitHubUser, gathererRef)
      twitterNode ! TwitterNodeQuery(gitHubUser, kloutNode, gathererRef)
    }
  }

  override def preStart() = {
    log.debug("[HeadNode] before starting...")
  }

  override def postStop() = {
    log.debug("[HeadNode] after stopping...")
  }
}
