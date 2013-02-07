package actors

import scala.util.{ Success, Failure }
import akka.actor.{ Actor, ActorRef, ActorLogging }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.linkedin._
import Messages._

class LinkedInNode extends Actor with ActorLogging {
  def receive = {
    case NodeQuery(searchedUser, gathererRef) => {
      log.debug("[LinkedInNode] receiving new head query")
      (for {
        firstname <- searchedUser.firstname
        lastname  <- searchedUser.lastname
      } yield {
        log.debug("[LinkedInNode] ok, firstname & lastname are valid")
        LinkedInAPI.searchByFullname(firstname, lastname) map {
          case Nil  => gathererRef ! NotFound("linkedin")
          case profils => LinkedIn.matchUser(searchedUser, profils) foreach { found =>
            gathererRef ! LinkedInResult(found)
          }
        } recover {
          case e: Exception => {
            log.error("[LinkedInNode] Error while fetching linkedIn profil: " + e.getMessage)
            gathererRef ! ErrorQuery("LinkedIn", e)
          }
        }
      }) getOrElse {
        log.info("[LinkedInNode] NotFound ! The firstname & lastname aren't valid: " + searchedUser)
        gathererRef ! NotFound("LinkedIn")
      }
    }
  }

  override def preStart() = {
    log.debug("[LinkedInNode] Starting ...")
  }

  override def postStop() = {
    log.debug("[LinkedInNode] after stopping...")
  }
}
