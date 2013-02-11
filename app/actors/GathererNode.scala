package actors

import scala.concurrent.duration._
import akka.actor.{ Actor, ActorRef, ActorLogging, ReceiveTimeout }
import play.api.libs.iteratee.Concurrent
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.{ CoderGuy, PopularCoder }
import utils.Config
import Messages._

class GathererNode(headNode: ActorRef) extends Actor with ActorLogging {
  self =>

  context.setReceiveTimeout(Config.gathererTimeout)

  val progress = Concurrent.broadcast[Double]
  private var clients: List[ActorRef] = Nil

  private var waited = Config.gathererWaited
  private var gitHubResult: Option[GitHubResult] = None
  private var kloutResult: Option[KloutResult] = None
  private var twitterResult: Option[TwitterResult] = None
  private var errors: Seq[(String, String)] = Nil

  def computeProgress(): Double = (100 - ((waited / Config.gathererWaited) * 100))

  def receive = {
    case AskProgress => {
      log.debug("[GathererNode] Ask progress")
      progress match {
        case (broadcaster, _) => sender ! broadcaster
      }
    }

    case NewClient(client) => {
      log.debug("[GathererNode] New client")
      clients = clients :+ client
    }

    case Decrement(nb) => {
      waited = waited - nb
      log.debug("[GatererNode] Decrementing to " + waited)
      val (_, pushHere) = progress
      pushHere.push(computeProgress())
      self ! CheckResult
    }

    case CheckResult => if(waited == 0) {
      log.info("[GathererNode] Gathering done !")
      val coderGuy = CoderGuy(
        gitHubResult map (_.gitHubUser),
        twitterResult map (_.twitterUser),
        kloutResult map (_.kloutUser),
        errors
      )
      PopularNode.ref ! UpdatePopular(coderGuy)
      clients foreach (_ ! coderGuy)
      headNode ! End(self)
    }

    case NotFound(message, notProcessed) => {
      log.info("[GathererNode] NotFound from %s well received".format(message))
      self ! Decrement(notProcessed)
    }

    case ErrorQuery(from, e, notProcessed) => {
      log.info("[GathererNode] Error well received: " + e.getMessage)
      errors = (from, e.getMessage) +: errors
      self ! Decrement(notProcessed)
    }

    case gr @ GitHubResult(profile) => {
      log.debug("[GathererNode] receiving github repositories")
      gitHubResult = Some(gr)
      self ! Decrement()
    }

    case kr @ KloutResult(profile) => {
      log.debug("[GathererNode] receiving klout influence")
      kloutResult = Some(kr)
      self ! Decrement()
    }

    case tr @ TwitterResult(profile) => {
      log.debug("[GathererNode] receiving twitter profile & timeline")
      twitterResult = Some(tr)
      self ! Decrement()
    }

    case ReceiveTimeout => {
      context.setReceiveTimeout(Duration.Undefined)
      log.error("[GathererNode] Timeout...")
      clients foreach (_ ! GathererException("Failed gathering all result : timeout"))
      headNode ! End(self)
    }
  }

  override def preStart() = {
    log.debug("[GathererNode] Starting ...")
  }

  override def postStop() = {
    log.debug("[GathererNode] after stopping...")
  }
}

case class GathererException(message: String) extends Exception
