package actors

import scala.util.{ Success, Failure }
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import akka.actor.{ Actor, ActorRef, Props, ActorLogging }
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumerator
import models.github.GitHubUser
import utils.Config
import Messages._

class HeadNode() extends Actor with ActorLogging {

  var currentRequests: Map[GitHubUser, ActorRef] = Map.empty
  var waitingClients: Map[GitHubUser, ActorRef] = Map.empty
  var waitingListeners: Map[String, List[ActorRef]] = Map.empty

  def gathererNode(client: ActorRef) = context.actorOf(Props(new GathererNode(self)))

  val gitHubNode = context.actorOf(Props[GitHubNode])
  val twitterNode = context.actorOf(Props[TwitterNode])
  val kloutNode = context.actorOf(Props[KloutNode])

  def receive = {

    case HeadQuery(user, client) => {
      log.debug("[HeadNode] receiving new init query")
      val gathererRef = gathererNode(client)
      (currentRequests.get(user), waitingListeners.get(user.login)) match {
        case (None, None) => waitingClients += (user -> client)
        case (None, Some(listeners)) => {
          currentRequests += (user -> gathererRef)
          launchSearch(gathererRef, user)
          waitingListeners -= user.login
          gathererRef ! NewClient(client)
          listeners foreach (giveProgressStream(gathererRef, _))
        }
        case (Some(_), _) => gathererRef ! NewClient(client)
      }
    }

    case AskProgress(login) => {
      log.debug("[HeadNode] AskProgress received")

      def byLogin(r: (GitHubUser, ActorRef)) = r match {
        case (user, _) => user.login == login
      }

      (currentRequests.find(byLogin), waitingClients.find(byLogin)) match {
        case (Some((_, gathererRef)), _) => giveProgressStream(gathererRef, sender)
        case (None, Some((user, client))) => {
          val gathererRef = gathererNode(client)
          currentRequests += (user -> gathererRef)
          waitingClients -= user
          launchSearch(gathererRef, user)
          giveProgressStream(gathererRef, sender)
          gathererRef ! NewClient(client)
        }
        case (None, None) => {
          waitingListeners.find { case (userName, _) => userName == login } match {
            case Some((user, listeners)) => waitingListeners += (user -> (sender :: listeners))
            case None => waitingListeners += (login -> List(sender))
          }
        }
      }
    }

    case End(gathererRef) => {
      log.debug("[HeadNode] End received !")
      val found  = currentRequests find { case (_, ref) =>  ref == gathererRef }
      if(found.isDefined) {
        currentRequests = currentRequests filter { case (_, ref) => ref != gathererRef }
        context.stop(gathererRef)
      }
    }
  }

  private def giveProgressStream(gathererRef: ActorRef, listener: ActorRef) = {
    implicit val timeout = Timeout(20 seconds)
    (gathererRef ? AskProgress).mapTo[Enumerator[Double]].map {
      case (progress: Enumerator[Double]) => {
        log.debug("[HeadNode] Ok, I received progress channel :)")
        listener ! progress
      }
    } recover {
      case e: Exception => {
        log.error("[HeadNode] failed getting progress")
        listener ! e
      }
    }
  }

  private def launchSearch(gathererRef: ActorRef, user: GitHubUser) = {
    gitHubNode   ! NodeQuery(user, gathererRef)
    twitterNode  ! TwitterNodeQuery(user, kloutNode, gathererRef)
  }

  override def supervisorStrategy =
    OneForOneStrategy(
      maxNrOfRetries = Config.headStrategyRetry,
      withinTimeRange = Config.headStrategyWithin
    ) {
      case e: Exception => {
        log.error("[HeadNode] catching exception : " + e.getMessage)
        Restart
      }
    }

  override def preStart() = {
    log.debug("[HeadNode] before starting...")
  }

  override def postStop() = {
    log.debug("[HeadNode] after stopping...")
  }
}
