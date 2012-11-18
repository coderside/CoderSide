package actors

import akka.actor.{ Actor, ActorRef, ActorLogging }
import play.api.libs.iteratee.Concurrent
import models.CoderGuy
import Messages._

class GathererNode(headNode: ActorRef) extends Actor with ActorLogging {
  self =>

  lazy val progress = Concurrent.broadcast[Float]
  private var clients: List[ActorRef] = Nil

  private var waited = 4
  private var gitHubResult: Option[GitHubResult] = None
  private var linkedInResult: Option[LinkedInResult] = None
  private var kloutResult: Option[KloutResult] = None
  private var twitterResult: Option[TwitterResult] = None

  def computeProgress(): Float = 100 - ((waited / 4F) * 100)

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

    case Decrement => {
      waited = waited - 1
      log.debug("[GatererNode] Decrementing to " + waited)
      val (_, pushHere) = progress
      pushHere.push(computeProgress())
      self ! CheckResult
    }

    case CheckResult => if(waited == 0) {
      log.info("[GathererNode] Gathering done !")
      val coderGuy = CoderGuy(
        gitHubResult map (_.repositories) getOrElse Nil,
        linkedInResult map(_.profil),
        twitterResult map (_.profil),
        twitterResult flatMap (_.timeline),
        kloutResult map (_.profil),
        kloutResult map (_.influencers) getOrElse Nil,
        kloutResult map (_.influencees) getOrElse Nil
      )
      clients foreach (_ ! coderGuy)
      headNode ! End(self)
    }

    case NotFound => {
      log.info("[GathererNode] NotFound well received")
      self ! Decrement
    }

    case ErrorQuery(e) => {
      log.info("[GathererNode] Error well received.")
      clients.foreach(client => client ! e)
    }

    case gr @ GitHubResult(repositories) => {
      log.debug("[GathererNode] receiving github repositories")
      gitHubResult = Some(gr)
      self ! Decrement
    }

    case lr @ LinkedInResult(profil) => {
      log.debug("[GathererNode] receiving linkedIn profil")
      linkedInResult = Some(lr)
      self ! Decrement
    }

    case kr @ KloutResult(user, influencers, influencees) => {
      log.debug("[GathererNode] receiving klout influence")
      kloutResult = Some(kr)
      self ! Decrement
    }

    case tr @ TwitterResult(profil, timeline) => {
      log.debug("[GathererNode] receiving twitter profil & timeline")
      twitterResult = Some(tr)
      self ! Decrement
    }
  }

  override def preStart() = {
    log.debug("[GathererNode] Starting ...")
  }

  override def postStop() = {
    log.debug("[GathererNode] after stopping...")
  }
}
