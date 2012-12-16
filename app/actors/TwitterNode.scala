package actors

import scala.util.{ Success, Failure, Try }
import akka.actor.{ Actor, ActorRef, ActorLogging }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
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

      def handleResponse(response: Try[List[TwitterUser]], notFound: => Unit)  = {
        response match {
          case Success(Nil) => notFound
          case Success(profils) => Twitter.matchUser(gitHubUser, profils) foreach { found =>
            self ! TwitterTimelineQuery(found, gathererRef)
            kloutRef ! KloutNodeQuery(found, gathererRef)
          }
          case Failure(e) => {
            log.error("[TwitterNode] Error while searching twitter user")
            gathererRef ! ErrorQuery("Twitter", e) //twitter
            gathererRef ! ErrorQuery("Klout", e) //klout
          }
        }
      }

      gitHubUser.fullname.filter(_ => gitHubUser.isFullnameOk) map { fname =>
        TwitterAPI.searchBy(fname).onComplete { byFullname =>
          handleResponse(byFullname,
            TwitterAPI.searchBy(gitHubUser.username).onComplete { byUsername =>
              handleResponse(byUsername, {
                  gathererRef ! NotFound("Twitter")
                  gathererRef ! NotFound("Klout")
                }
              )
            }
          )
        }
      } getOrElse {
        TwitterAPI.searchBy(gitHubUser.username).onComplete { byUsername =>
          handleResponse(byUsername, {
            gathererRef ! NotFound("Twitter")
            gathererRef ! NotFound("Klout")
          })
        }
      }
    }

    case TwitterTimelineQuery(twitterUser, gathererRef) => {
      log.debug("[TwitterNode] Getting twitter timeline")
      TwitterAPI.timeline(twitterUser.screenName).onComplete {
        case Success(timeline) => gathererRef ! TwitterResult(twitterUser.copy(timeline = timeline))
        case Failure(e) => {
          log.error("[TwitterNode] Error while fetching twitter user timeline")
          gathererRef ! ErrorQuery("Twitter", e)
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
