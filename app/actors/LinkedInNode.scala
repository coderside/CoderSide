package actors

import scala.util.{ Success, Failure }
import akka.actor.{ Actor, ActorRef, ActorLogging }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.linkedin._
import Messages._

class LinkedInNode extends Actor with ActorLogging {
  def receive = {
    case NodeQuery(gitHubUser, gathererRef) => {
      log.debug("[LinkedInNode] receiving new head query")
      (for {
        firstname <- gitHubUser.firstname
        lastname  <- gitHubUser.lastname
      } yield {
        log.debug("[LinkedInNode] ok, firstname & lastname are valid")
        LinkedInAPI.searchByFullname(firstname, lastname).onComplete {
          case Success(Nil)  => gathererRef ! NotFound("linkedin")
          case Success(profils) => LinkedIn.matchUser(gitHubUser, profils) foreach { found =>
            gathererRef ! LinkedInResult(found)
          }
          case Failure(e) => {
            log.error("[LinkedInNode] Error while fetching linkedIn profil")
            gathererRef ! ErrorQuery(e)
          }
        }
      }) getOrElse {
        log.info("[LinkedInNode] NotFound ! The firstname & lastname aren't valid: " + gitHubUser)
        gathererRef ! NotFound
      }
    }
  }
}
