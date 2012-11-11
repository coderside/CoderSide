package actors

import scala.util.{ Success, Failure }
import scala.concurrent.duration._
import akka.actor.{ Actor, ActorRef, Props, ActorLogging }
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumerator
import models.github.GitHubUser
import Messages._

class HeadNode() extends Actor with ActorLogging {

  var requests: Map[GitHubUser, ActorRef] = Map.empty
  def gathererNode(client: ActorRef) = context.actorOf(Props(new GathererNode(self)))

  lazy val gitHubNode = context.actorOf(Props[GitHubNode])
  lazy val linkedInNode = context.actorOf(Props[LinkedInNode])
  lazy val twitterNode = context.actorOf(Props[TwitterNode])
  lazy val kloutNode = context.actorOf(Props[KloutNode])

  def receive = {
    case HeadQuery(gitHubUser, client) => {
      log.debug("[HeadNode] receiving new init query")
      val gathererRef = gathererNode(client)
      if(!requests.get(gitHubUser).isDefined) {
        log.debug("[HeadNode] Request added")
        requests += (gitHubUser -> gathererRef)
        gitHubNode   ! NodeQuery(gitHubUser, gathererRef)
        linkedInNode ! NodeQuery(gitHubUser, gathererRef)
        twitterNode  ! TwitterNodeQuery(gitHubUser, kloutNode, gathererRef)
      }
      requests.get(gitHubUser).foreach { gathererRef =>
        gathererRef ! NewClient(client)
      }
    }

    case AskProgress(gitHubUser) => {
      implicit val timeout = Timeout(20 seconds)
      val s = sender
      log.debug("[HeadNode] AskProgress received")
      def requestProgress(retries: Int) {
        requests.get(gitHubUser).map { gathererRef =>
          log.debug("[HeadNode] Ok, ask for progress channel")
          (gathererRef ? AskProgress).mapTo[Enumerator[Float]].onComplete {
            case Success(progress: Enumerator[Float]) => {
              log.debug("[HeadNode] Ok, I received progress channel :)")
              s ! progress
            }
            case Failure(e) => {
              log.error("[HeadNode] failed getting progress")
              sender ! e
            }
          }
        } getOrElse {
          if(retries > 0) {
            log.debug("[HeadNode] Retrying to get progress channel: " + retries)
            context.system.scheduler.scheduleOnce(1.seconds)(requestProgress(retries - 1))
          } else s ! new Exception("Can't find progress channel")
        }
      }
      requestProgress(3)
    }

    case End(gathererRef) => {
      log.debug("[HeadNode] End received !")
      val found  = requests.find(_._2 == gathererRef)
      if(found.isDefined) {
        requests = requests.filter(_._2 != gathererRef)
        context.stop(gathererRef)
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
