package actors

import scala.util.{ Success, Failure }
import akka.actor.{ Actor, ActorRef, ActorLogging }
import play.api.libs.concurrent.Promise
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.github._
import Messages._

class GitHubNode extends Actor with ActorLogging {
  def receive = {
    case NodeQuery(gitHubUser, gathererRef) => {
      log.debug("[GitHubNode] receiving new head query")
      GitHubAPI.repositoriesByUser(gitHubUser.username).onComplete {
        case Success(repos) => self ! GitHubOrgQuery(gitHubUser.copy(repositories = repos), gathererRef)
        case Failure(e) => {
          log.error("[GitHubNode] Error while fetching user repositories")
          gathererRef ! ErrorQuery(e)
        }
      }
    }
    case GitHubOrgQuery(gitHubUser, gathererRef) => {
      log.debug("[GitHubNode] receiving GitHub organization query")
      GitHubAPI.organizations(gitHubUser.username).onComplete {
        case Success(organizations) =>
          Promise.sequence(
            organizations.map { org =>
              GitHubAPI.repositoriesByOrg(org.login).map { repos =>
                org.copy(repositories = repos)
              }
            }
          ) onComplete {
            case Success(orgs) => gathererRef ! GitHubResult(gitHubUser.copy(organizations = orgs))
            case Failure(e) => {
              log.error("[GitHubNode] Error while fetching organization repositories")
              gathererRef ! ErrorQuery(e)
            }
          }
        case Failure(e) => {
          log.error("[GitHubNode] Error while fetching user organizations")
          gathererRef ! ErrorQuery(e)
        }
      }
    }
  }

  override def preStart() = {
    log.debug("[GitHubNode] Starting ...")
  }

  override def postStop() = {
    log.debug("[GitHubNode] after stopping...")
  }
}
