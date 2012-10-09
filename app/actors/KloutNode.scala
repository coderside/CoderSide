package actors

import scala.concurrent.Future
import scala.concurrent.util.duration._
import akka.actor.{ Actor, ActorRef, ActorLogging }
import play.api.libs.concurrent.execution.defaultContext
import play.api.libs.concurrent.Promise
import models.klout._
import models.twitter._
import Messages._

class KloutNode extends Actor with ActorLogging {
  def receive = {
    case KloutNodeQuery(twitterUser, gathererRef) => {
      log.debug("[KloutNode] receiving new query : " + twitterUser)
      KloutAPI.kloutID(twitterUser.screenName).onComplete {
        case Right(Some(kloutID)) => self ! KloutUserQuery(kloutID, gathererRef)
        case Right(None) => gathererRef ! NotFound
        case Left(e) => {
          log.error("[KloutNode] Error while getting klout ID")
          gathererRef ! ErrorQuery(e)
        }
      }
    }
    case KloutUserQuery(kloutID, gathererRef) => {
      log.debug("[KloutNode] Getting profil influence with: " + kloutID)

      KloutAPI.influence(kloutID).onComplete {
        case Right(Influence(influencers, influencees)) => {
          def splitInfluence(influence: Set[TwitterUser]) = {
            influence.partition(twitterUser => influencers.find(_.nick == twitterUser.screenName).isDefined)
          }
          Promise.sequence(
            (influencers ++ influencees).map( k => TwitterAPI.show(k.nick))
          ).onComplete {
            case Right(influence) => splitInfluence(influence.flatten) match {
              case (influencers, influencees) => gathererRef ! KloutResult(influencers, influencees)
            }
            case Left(e) => gathererRef ! ErrorQuery(e)
          }
        }
        case Left(e) => {
          log.error("[KloutNode] Error while getting klout ID")
          gathererRef ! ErrorQuery(e)
        }
      }
    }
  }
}
