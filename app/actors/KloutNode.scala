package actors

import scala.concurrent.Future
import scala.util.{ Success, Failure }
import akka.actor.{ Actor, ActorRef, ActorLogging }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Promise
import models.klout._
import models.twitter._
import Messages._

class KloutNode extends Actor with ActorLogging {
  def receive = {
    case KloutNodeQuery(twitterUser, gathererRef) => {
      log.debug("[KloutNode] receiving new query")
      KloutAPI.kloutID(twitterUser.screenName) map {
        case Some(kloutID) => {
          KloutAPI.kloutUser(kloutID) map {
            case Some(kloutUser) => self ! KloutUserQuery(kloutUser, gathererRef)
            case None => gathererRef ! NotFound("Klout")
          } recover {
            case e: Exception => {
              log.error("[KloutNode] Error while getting klout user: " + e.getMessage)
              gathererRef ! ErrorQuery("Klout", e)
            }
          }
        }
        case None => gathererRef ! NotFound("Klout")
      } recover {
        case e: Exception => {
          log.error("[KloutNode] Error while getting klout ID: " + e.getMessage)
          gathererRef ! ErrorQuery("Klout", e)
        }
      }
    }

    case KloutUserQuery(kloutUser, gathererRef) => {
      log.debug("[KloutNode] Getting profile influence for " + kloutUser.id)
      KloutAPI.influence(kloutUser.id) map {
        case Influence(influencers, influencees) => {
          Promise.sequence(
            (influencers ++ influencees) map (k => TwitterAPI.show(k.nick) map (k.id -> _))
          ) map {
            case twitterUsers =>
              Klout.splitInfluence(influencers, influencees, Klout.flattenTwitterUsers(twitterUsers)) match {
                case (influencers, influencees) =>
                  val res = KloutResult(kloutUser.copy(influencers = influencers, influencees = influencees))
                  gathererRef ! res
              }
          } recover {
            case e: Exception => {
              log.error("[KloutNode] Error while getting influence (second part): " + e.getMessage)
              gathererRef ! ErrorQuery("Klout", e)
            }
          }
        }
      } recover {
        case e: Exception => {
          log.error("[KloutNode] Error while getting influence ID (first part): " + e.getMessage)
          gathererRef ! ErrorQuery("Klout", e)
        }
      }
    }
  }

  override def preStart() = {
    log.debug("[KloutNode] Starting ...")
  }

  override def postStop() = {
    log.debug("[KloutNode] after stopping...")
  }
}
