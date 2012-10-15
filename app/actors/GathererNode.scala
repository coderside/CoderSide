package actors

import akka.actor.{ Actor, ActorRef, ActorLogging }
import scala.concurrent.util.duration._
import play.api.libs.iteratee.Concurrent
import models.CoderGuy
import Messages._

class GathererNode(client: ActorRef) extends Actor with ActorLogging {
  self =>

  lazy val progress = Concurrent.broadcast[Float]

  private var otherClients: List[ActorRef] = Nil
  private var waited = 4
  private var gitHubResult: Option[GitHubResult] = None
  private var linkedInResult: Option[LinkedInResult] = None
  private var kloutResult: Option[KloutResult] = None
  private var twitterResult: Option[TwitterResult] = None

  def computeProgress(): Float = 100 - ((waited / 4) * 100)

  def receive = {

    case AskProgress => {
      log.debug("[GathererNode] Ask progressz")
      progress match {
        case (broadcaster, _) => sender ! broadcaster
      }
    }

    case NewClient(client) => {
      log.debug("[GathererNode] New client")
      otherClients :+ client
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
        gitHubResult map(gr => gr.repositories) getOrElse Set.empty,
        linkedInResult.map(lr => lr.profil),
        twitterResult.map(tr => tr.profil),
        twitterResult.map(tr => tr.timeline),
        kloutResult map(kr => kr.influencers) getOrElse Set.empty,
        kloutResult map(kr => kr.influencees) getOrElse Set.empty
      )
      client ! coderGuy
      otherClients.foreach(client => client ! coderGuy)
      context.stop(self)
    }

    case NotFound => {
      log.info("[GathererNode] NotFound well received")
      self ! Decrement
    }

    case ErrorQuery(e) => {
      log.info("[GathererNode] Error well received.")
      client ! e
    }

    case gr @ GitHubResult(repositories) => {
      log.debug("[GathererNode] receiving github repositories: " + repositories)
      gitHubResult = Some(gr)
      self ! Decrement
    }

    case lr @ LinkedInResult(profil) => {
      log.debug("[GathererNode] receiving linkedIn profil: " + profil)
      linkedInResult = Some(lr)
      self ! Decrement
    }

    case kr @ KloutResult(influencers, influencees) => {
      log.debug("[GathererNode] receiving klout influence: " + kr)
      kloutResult = Some(kr)
      self ! Decrement
    }

    case tr @ TwitterResult(profil, timeline) => {
      log.debug("[GathererNode] receiving twitter profil & timeline: " + tr)
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
