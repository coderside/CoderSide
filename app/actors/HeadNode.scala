package actors

import scala.util.{ Success, Failure }
import scala.concurrent.util.duration._
import akka.actor.{ Actor, ActorRef, Props, ActorLogging }
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.concurrent.execution.defaultContext
import Messages._

class HeadNode() extends Actor with ActorLogging {

  var gatherers: Map[String, ActorRef] = Map.empty
  def gathererNode(client: ActorRef) = context.actorOf(Props(new GathererNode(client)))

  lazy val gitHubNode = context.actorOf(Props[GitHubNode])
  lazy val linkedInNode = context.actorOf(Props[LinkedInNode])
  lazy val twitterNode = context.actorOf(Props[TwitterNode])
  lazy val kloutNode = context.actorOf(Props[KloutNode])

  def receive = {
    case HeadQuery(request, gitHubUser, client) => {
      log.debug("[HeadNode] receiving new init query : " + gitHubUser)
      val gathererRef = gathererNode(client)
      if(!gatherers.find(_._1 == request).isDefined) {
        gatherers += (request -> gathererRef)
        gitHubNode ! NodeQuery(gitHubUser, gathererRef)
        linkedInNode ! NodeQuery(gitHubUser, gathererRef)
        twitterNode ! TwitterNodeQuery(gitHubUser, kloutNode, gathererRef)
      } else {
        gatherers.get(request).foreach { gathererRef =>
          gathererRef ! NewClient(client)
        }
      }
    }

    case AskProgress(request) => {
      implicit val timeout = Timeout(20.seconds)
      gatherers.get(request).foreach { gathererRef =>
        (gathererRef ? AskProgress).onComplete {
          case Success(progress) => sender ! progress
          case Failure(e) => {
            log.error("[HeadNode] failed getting progress")
            sender ! e
          }
        }
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
