package actors

import scala.concurrent.util.duration._
import akka.actor.{ Actor, ActorRef, ActorLogging }
import play.api.libs.concurrent.execution.defaultContext
import models.linkedin._
import Messages._

class LinkedInNode extends Actor with ActorLogging {
  def receive = {
    case NodeQuery(gitHubUser, gathererRef) => {
      log.debug("[LinkedInNode] receiving new head query : " + gitHubUser)
      LinkedInAPI.searchByFullname(gitHubUser.firstname, gitHubUser.lastname).onComplete {
        case Right(profils) if(profils.size > 0) => gathererRef ! LinkedInResult(profils.head)
        case Right(profils) if(profils.size == 0) => gathererRef ! NotFound
        case Left(e) => {
          log.error("[LinkedInNode] Error while fetching repositories")
          gathererRef ! ErrorQuery(e)
        }
      }
    }
  }
}
