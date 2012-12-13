package actors

import scala.concurrent.Future
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
          gathererRef ! ErrorQuery("GitHub", e)
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
            case Success(orgs) => self ! GitHubContribQuery(gitHubUser.copy(organizations = orgs), gathererRef)
            case Failure(e) => {
              log.error("[GitHubNode] Error while fetching organization repositories")
              gathererRef ! ErrorQuery("GitHub", e)
            }
          }
        case Failure(e) => {
          log.error("[GitHubNode] Error while fetching user organizations")
          gathererRef ! ErrorQuery("GitHub", e)
        }
      }
    }

    case GitHubContribQuery(gitHubUser, gathererRef) => {
      log.debug("[GitHubNode] receiving GitHub contribution query")
      def contributions(repositories: List[GitHubRepository]): Future[List[GitHubRepository]] = {
        Promise.sequence(
          repositories map { repository =>
            GitHubAPI.contributions(gitHubUser.username, repository) map { contrib =>
              repository.copy(contributions = contrib)
            }
          }
        )
      }
      Promise.sequence(
        gitHubUser.organizations map { org =>
          contributions(org.repositories) map { repos =>
            org.copy(repositories =  repos)
          }
        }
      ) onComplete {
        case Success(orgs) => {
          contributions(gitHubUser.repositories) onComplete {
            case Success(repos) => gathererRef ! GitHubResult(gitHubUser.copy(organizations = orgs, repositories = repos))
            case Failure(e) => {
              log.error("[GitHubNode] Error while fetching contribution for organizations repositories")
              gathererRef ! ErrorQuery("GitHub", e)
            }
          }
        }
        case Failure(e) => {
          log.error("[GitHubNode] Error while fetching contribution for his repositories")
          gathererRef ! ErrorQuery("GitHub", e)
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
