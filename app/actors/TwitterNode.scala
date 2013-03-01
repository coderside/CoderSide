package actors

import scala.util.{ Success, Failure, Try }
import akka.actor.{ Actor, ActorRef, ActorLogging }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import models.twitter._
import Messages._

class TwitterNode extends Actor with ActorLogging {

  def receive = {
    case TwitterNodeQuery(user, kloutRef, gathererRef) => {
      log.debug("[TwitterNode] receiving new head query")
      gathererRef ! Decrement()
      (for {
        socialProfileOpt <- user.socialProfile
        if(socialProfileOpt.isDefined)
        twitterProfileOpt <- socialProfileOpt.get.twitterProfile
        if(twitterProfileOpt.isDefined)
      } yield {
        log.debug("### Ok, Using Fullcontact ! ###")
        gathererRef ! Decrement()
        self ! TwitterTimelineQuery(twitterProfileOpt.get, gathererRef)
        kloutRef ! KloutNodeQuery(twitterProfileOpt.get, gathererRef)
      }).recover {
        case e: NoSuchElementException => {
          log.debug("### We don't use fullcontact so I hope the matching will be correct ###")
          self ! TwitterUserQuery(user, kloutRef, gathererRef)
        }
        case e: Exception => {
          log.error("[TwitterNode] Error while searching twitter user: " + e.getMessage)
          gathererRef ! ErrorQuery("Twitter", e, 2) //twitter
          gathererRef ! ErrorQuery("Klout", e, 2) //klout
        }
      }
    }

    case TwitterUserQuery(user, kloutRef, gathererRef) => {
      log.debug("[TwitterNode] Getting twitter profile")

      def handleResponse(response: List[TwitterUser], notFound: => Unit) = {
        response match {
          case Nil => notFound
          case profiles => Twitter.matchUser(user, profiles) foreach { found =>
            gathererRef ! Decrement()
            self ! TwitterTimelineQuery(found, gathererRef)
            kloutRef ! KloutNodeQuery(found, gathererRef)
          }
        }
      }

      val handleSearchError: PartialFunction[Throwable, Unit] = {
        case e: Exception => {
          log.error("[TwitterNode] Error while searching twitter user: " + e.getMessage)
          gathererRef ! ErrorQuery("Twitter", e, 2) //twitter
          gathererRef ! ErrorQuery("Klout", e, 2) //klout
        }
      }

      user.name.map { fname =>
        TwitterAPI.searchBy(fname).map { byFullname =>
          handleResponse(
            byFullname,
            TwitterAPI.searchBy(user.login) map { byUsername =>
              handleResponse(byUsername, {
                gathererRef ! NotFound("Twitter", 2)
                gathererRef ! NotFound("Klout", 2)
              }
              )
            } recover(handleSearchError)
          )
        } recover(handleSearchError)
      } getOrElse {
        TwitterAPI.searchBy(user.login) map { byUsername =>
          handleResponse(
            byUsername, {
              gathererRef ! NotFound("Twitter", 2)
              gathererRef ! NotFound("Klout", 2)
            })
        } recover(handleSearchError)
      }
    }

    case TwitterTimelineQuery(twitterUser, gathererRef) => {
      log.debug("[TwitterNode] Getting twitter timeline")
      TwitterAPI.timeline(twitterUser.screenName) map { timeline =>
        gathererRef ! TwitterResult(twitterUser.copy(timeline = timeline))
      } recover {
        case e: Exception => {
          log.error("[TwitterNode] Error while fetching twitter user timeline: " + e.getMessage)
          gathererRef ! ErrorQuery("Twitter", e, 1)
        }
      }
    }
  }

  override def preStart() = {
    log.debug("[TwitterNode] Starting ...")
  }

  override def postStop() = {
    log.debug("[TwitterNode] after stopping...")
  }
}
