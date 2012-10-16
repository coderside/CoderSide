package actors

import scala.concurrent.Future
import scala.concurrent.util.duration._
import scala.util.{ Success, Failure }
import akka.actor.{ Actor, ActorRef, ActorLogging }
import play.api.libs.concurrent.execution.defaultContext
import play.api.libs.concurrent.Promise
import models.klout._
import models.twitter._
import Messages._

class KloutNode extends Actor with ActorLogging {
  def receive = {
    case KloutNodeQuery(twitterUser, gathererRef) => {
      log.debug("[KloutNode] receiving new query")
      KloutAPI.kloutID(twitterUser.screenName).onComplete {
        case Success(Some(kloutID)) => self ! KloutUserQuery(kloutID, gathererRef)
        case Success(None) => gathererRef ! NotFound
        case Failure(e) => {
          log.error("[KloutNode] Error while getting klout ID: " + e.getMessage)
          gathererRef ! ErrorQuery(e)
        }
      }
    }
    case KloutUserQuery(kloutID, gathererRef) => {
      log.debug("[KloutNode] Getting profil influence")
      KloutAPI.influence(kloutID).onComplete {
        case Success(Influence(influencers, influencees)) => {
          def splitInfluence(influence: Set[TwitterUser]) = {
            influence.partition(twitterUser => influencers.find(_.nick == twitterUser.screenName).isDefined)
          }
          Promise.sequence(
            (influencers ++ influencees).map( k => TwitterAPI.show(k.nick))
          ).onComplete {
            case Success(influence) => splitInfluence(influence.flatten) match {
              case (influencers, influencees) => gathererRef ! KloutResult(influencers, influencees)
            }
            case Failure(e) => {
              log.error("[KloutNode] Error while getting influence (second part): " + e.getMessage)
              gathererRef ! ErrorQuery(e)
            }
          }
        }
        case Failure(e) => {
          log.error("[KloutNode] Error while getting influence ID (first part): " + e.getMessage)
          gathererRef ! ErrorQuery(e)
        }
      }
    }
  }
}
