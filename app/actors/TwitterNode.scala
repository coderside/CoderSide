package actors

import scala.util.{ Success, Failure }
import akka.actor.{ Actor, ActorRef, ActorLogging }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.twitter._
import Messages._

class TwitterNode extends Actor with ActorLogging {
  def receive = {
    case TwitterNodeQuery(gitHubUser, kloutRef, gathererRef) => {
      log.debug("[TwitterNode] receiving new head query")
      self ! TwitterUserQuery(gitHubUser, kloutRef, gathererRef)
    }

    case TwitterUserQuery(gitHubUser, kloutRef, gathererRef) => {
      log.debug("[TwitterNode] Getting twitter profil")
      TwitterAPI.searchByFullname(gitHubUser.fullname).onComplete {
        case Success(Nil) => gathererRef ! NotFound
        case Success(profils) =>
          Twitter.matchUser(gitHubUser, profils) foreach { found =>
            self ! TwitterTimelineQuery(found, gathererRef)
            kloutRef ! KloutNodeQuery(found, gathererRef)
          }
        case Failure(e) => {
          log.error("[TwitterNode] Error while searching twitter user")
          gathererRef ! ErrorQuery(e)
        }
      }
    }

    case TwitterTimelineQuery(twitterUser, gathererRef) => {
      log.debug("[TwitterNode] Getting twitter timeline")
      TwitterAPI.timeline(twitterUser.screenName).onComplete {
        case Success(Some(timeline)) => gathererRef ! TwitterResult(twitterUser, timeline)
        case Success(None) => gathererRef ! NotFound
        case Failure(e) => {
          log.error("[TwitterNode] Error while fetching twitter user timeline")
          gathererRef ! ErrorQuery(e)
        }
      }
    }
  }
}
