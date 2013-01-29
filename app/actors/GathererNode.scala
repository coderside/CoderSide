package actors

import scala.concurrent.duration._
import akka.actor.{ Actor, ActorRef, ActorLogging, ReceiveTimeout }
import play.api.libs.iteratee.Concurrent
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import reactivemongo.bson.BSONObjectID
import models.{ CoderGuy, PopularCoder }
import utils.Config
import Messages._

class GathererNode(headNode: ActorRef) extends Actor with ActorLogging {
  self =>

  context.setReceiveTimeout(Config.gathererTimeout)

  val progress = Concurrent.broadcast[Float]
  private var clients: List[ActorRef] = Nil

  private var waited = Config.gathererWaited
  private var gitHubResult: Option[GitHubResult] = None
  private var linkedInResult: Option[LinkedInResult] = None
  private var kloutResult: Option[KloutResult] = None
  private var twitterResult: Option[TwitterResult] = None
  private var errors: Seq[(String, String)] = Nil

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
        gitHubResult map (_.profil),
        linkedInResult map(_.profil),
        twitterResult map (_.profil),
        kloutResult map (_.profil),
        errors
      )

      gitHubResult.foreach { github =>
        import PopularCoder.json._
        val fullname = linkedInResult.map(_.profil.fullName) orElse twitterResult.map(_.profil.name) orElse github.profil.fullname
        val popularCoder = PopularCoder(
          BSONObjectID.generate,
          github.profil.username,
          fullname,
          twitterResult map(_.profil.description)
        )
        PopularCoder.findByPseudo(github.profil.username)
        .collect { case Some(coderAsJson) => coderAsJson }
        .map (_.asOpt[PopularCoder])
        .collect { case Some(coder) => coder } map { coder =>
          coder.increment()
        } recover {
          case e: NoSuchElementException => PopularCoder.uncheckedCreate(popularCoder)
          case e: Exception => log.error("Error while saving popular coder info: " + e.getMessage)
        }
      }

      clients foreach (_ ! coderGuy)
      headNode ! End(self)
    }

    case NotFound(message) => {
      log.info("[GathererNode] NotFound from %s well received".format(message))
      self ! Decrement
    }

    case ErrorQuery(from, e) => {
      log.info("[GathererNode] Error well received: " + e.getMessage)
      errors = (from, e.getMessage) +: errors
      self ! Decrement
    }

    case gr @ GitHubResult(profil) => {
      log.debug("[GathererNode] receiving github repositories")
      gitHubResult = Some(gr)
      self ! Decrement
    }

    case lr @ LinkedInResult(profil) => {
      log.debug("[GathererNode] receiving linkedIn profil")
      linkedInResult = Some(lr)
      self ! Decrement
    }

    case kr @ KloutResult(profil) => {
      log.debug("[GathererNode] receiving klout influence")
      kloutResult = Some(kr)
      self ! Decrement
    }

    case tr @ TwitterResult(profil) => {
      log.debug("[GathererNode] receiving twitter profil & timeline")
      twitterResult = Some(tr)
      self ! Decrement
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
