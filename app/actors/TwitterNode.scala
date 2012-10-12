package actors

import scala.concurrent.util.duration._
import akka.actor.{ Actor, ActorRef, ActorLogging }
import play.api.libs.concurrent.execution.defaultContext
import models.twitter._
import Messages._

class TwitterNode extends Actor with ActorLogging {
  def receive = {
    case TwitterNodeQuery(gitHubUser, kloutRef, gathererRef) => {
      log.debug("[TwitterNode] receiving new head query : " + gitHubUser)
      self ! TwitterUserQuery(gitHubUser, kloutRef, gathererRef)
    }

    case TwitterUserQuery(gitHubUser, kloutRef, gathererRef) => {
      log.debug("[TwitterNode] Getting twitter profil with: " + gitHubUser)
      TwitterAPI.searchByFullname(gitHubUser.fullname).onComplete {
        case Right(profils) if(profils.size > 0) => {
          self ! TwitterTimelineQuery(profils.head, gathererRef)
          kloutRef ! KloutNodeQuery(profils.head, gathererRef)
        }
        case Right(profils) if(profils.size == 0) => gathererRef ! NotFound
        case Left(e) => {
          log.error("[TwitterNode] Error while searching twitter user")
          gathererRef ! ErrorQuery(e)
        }
      }
    }

    case TwitterTimelineQuery(twitterUser, gathererRef) => {
      log.debug("[TwitterNode] Getting twitter timeline with: " + twitterUser)
      TwitterAPI.timeline(twitterUser.screenName).onComplete {
        case Right(Some(timeline)) => gathererRef ! TwitterResult(twitterUser, timeline)
        case Right(None) => gathererRef ! NotFound
        case Left(e) => {
          log.error("[TwitterNode] Error while fetching twitter user timeline")
          gathererRef ! ErrorQuery(e)
        }
      }
    }
  }
}
