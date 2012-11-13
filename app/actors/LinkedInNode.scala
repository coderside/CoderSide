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
      LinkedInAPI.searchByFullname(gitHubUser.firstname, gitHubUser.lastname).onComplete {
        case Success(Nil)  => gathererRef ! NotFound
        case Success(profils) => LinkedIn.matchUser(gitHubUser, profils) foreach { found =>
          gathererRef ! LinkedInResult(found)
        }
        case Failure(e) => {
          log.error("[LinkedInNode] Error while fetching repositories")
          gathererRef ! ErrorQuery(e)
        }
      }
    }
  }
}
