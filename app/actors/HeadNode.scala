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
import models.github.GitHubSearchedUser
import utils.Config
import Messages._

class HeadNode() extends Actor with ActorLogging {

  var currentRequests: Map[GitHubSearchedUser, ActorRef] = Map.empty
  var waitingClients: Map[GitHubSearchedUser, ActorRef] = Map.empty
  var waitingListeners: Map[GitHubSearchedUser, List[ActorRef]] = Map.empty

  def gathererNode(client: ActorRef) = context.actorOf(Props(new GathererNode(self)))

  val gitHubNode = context.actorOf(Props[GitHubNode])
  val twitterNode = context.actorOf(Props[TwitterNode])
  val kloutNode = context.actorOf(Props[KloutNode])

  def receive = {

    case HeadQuery(searchedUser, client) => {
      log.debug("[HeadNode] receiving new init query")
      val gathererRef = gathererNode(client)
      (currentRequests.get(searchedUser), waitingListeners.get(searchedUser)) match {
        case (None, None) => waitingClients += (searchedUser -> client)
        case (None, Some(listeners)) => {
          currentRequests += (searchedUser -> gathererRef)
          launchSearch(gathererRef, searchedUser)
          waitingListeners -= searchedUser
          gathererRef ! NewClient(client)
          listeners foreach (giveProgressStream(gathererRef, _))
        }
        case (Some(_), _) => gathererRef ! NewClient(client)
      }
    }

    case AskProgress(searchedUser) => {
      log.debug("[HeadNode] AskProgress received")
      (currentRequests.get(searchedUser), waitingClients.get(searchedUser)) match {
        case (Some(gathererRef), _) => giveProgressStream(gathererRef, sender)
        case (None, Some(client)) => {
          val gathererRef = gathererNode(client)
          currentRequests += (searchedUser -> gathererRef)
          waitingClients -= searchedUser
          launchSearch(gathererRef, searchedUser)
          giveProgressStream(gathererRef, sender)
          gathererRef ! NewClient(client)
        }
        case (None, None) => {
          waitingListeners.get(searchedUser) match {
            case Some(listeners) => waitingListeners += (searchedUser -> (sender :: listeners))
            case None => waitingListeners += (searchedUser -> List(sender))
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

  private def launchSearch(gathererRef: ActorRef, searchedUser: GitHubSearchedUser) = {
    gitHubNode   ! NodeQuery(searchedUser, gathererRef)
    twitterNode  ! TwitterNodeQuery(searchedUser, kloutNode, gathererRef)
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
