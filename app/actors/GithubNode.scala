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
      GitHubAPI.repositoriesByUser(gitHubUser.username) map { repos =>
        self ! GitHubOrgQuery(gitHubUser.copy(repositories = repos), gathererRef)
      } recover {
        case e: Exception => {
          log.error("[GitHubNode] Error while fetching user repositories")
          gathererRef ! ErrorQuery("GitHub", e)
        }
      }
    }

    case GitHubOrgQuery(gitHubUser, gathererRef) => {
      log.debug("[GitHubNode] receiving GitHub organization query")
      GitHubAPI.organizations(gitHubUser.username) map { organizations =>
        Promise.sequence(
          organizations.map { org =>
            GitHubAPI.repositoriesByOrg(org.login).map { repos =>
              org.copy(repositories = repos)
            }
          }
        ) map { orgs =>
          self ! GitHubContribQuery(gitHubUser.copy(organizations = orgs), gathererRef)
        } recover {
          case e: Exception => {
            log.error("[GitHubNode] Error while fetching user organizations repos: " + e.getMessage)
            gathererRef ! ErrorQuery("GitHub", e)
          }
        }
      } recover {
        case e: Exception => {
          log.error("[GitHubNode] Error while fetching user organizations: " + e.getMessage)
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
            org.copy(repositories = repos)
          }
        }
      ) map { orgs =>
        contributions(gitHubUser.repositories) map { repos =>
          gathererRef ! GitHubResult(gitHubUser.copy(organizations = orgs, repositories = repos))
        } recover {
          case e: Exception => {
            e.printStackTrace
            log.error("[GitHubNode] Error while fetching contribution for his repositories: " + e.getMessage)
            gathererRef ! ErrorQuery("GitHub", e)
          }
        }
      } recover {
        case e: Exception => {
          e.printStackTrace
          log.error("[GitHubNode] Error while fetching contribution for his repositories: " + e.getMessage)
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
