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

    case NodeQuery(searchedUser, gathererRef) => {
      log.debug("[GitHubNode] receiving new head query")
      (for {
        maybeProfile <- GitHubAPI.profile(searchedUser.login)
        if(maybeProfile.isDefined)
        repos <- GitHubAPI.repositoriesByUser(searchedUser.login)
      } yield {
        val profileWithRepos = maybeProfile.get.copy(repositories = repos, language = searchedUser.language)
        gathererRef ! Decrement()
        self ! GitHubOrgQuery(profileWithRepos, gathererRef)
      }) recover {
        case e: Exception => {
          log.error("[GitHubNode] Error while fetching user repositories")
          gathererRef ! ErrorQuery("GitHub", e, 3)
        }
      }
    }

    case GitHubOrgQuery(gitHubUser, gathererRef) => {
      log.debug("[GitHubNode] receiving GitHub organization query")
      GitHubAPI.organizations(gitHubUser.login) map { organizations =>
        Promise.sequence(
          organizations.map { org =>
            GitHubAPI.repositoriesByOrg(org.login).map { repos =>
              org.copy(repositories = repos)
            }
          }
        ) map { orgs =>
          self ! GitHubContribQuery(gitHubUser.copy(organizations = orgs), gathererRef)
          gathererRef ! Decrement()
        } recover {
          case e: Exception => {
            log.error("[GitHubNode] Error while fetching user organizations repos: " + e.getMessage)
            gathererRef ! ErrorQuery("GitHub", e, 2)
          }
        }
      } recover {
        case e: Exception => {
          log.error("[GitHubNode] Error while fetching user organizations: " + e.getMessage)
          gathererRef ! ErrorQuery("GitHub", e, 2)
        }
      }
    }

    case GitHubContribQuery(gitHubUser, gathererRef) => {
      log.debug("[GitHubNode] receiving GitHub contribution query")
      def contributions(repositories: List[GitHubRepository]): Future[List[GitHubRepository]] = {
        Promise.sequence(
          repositories map { repository =>
            GitHubAPI.contributions(gitHubUser.login, repository) map { contrib =>
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
            gathererRef ! ErrorQuery("GitHub", e, 1)
          }
        }
      } recover {
        case e: Exception => {
          e.printStackTrace
          log.error("[GitHubNode] Error while fetching contribution for his repositories: " + e.getMessage)
          gathererRef ! ErrorQuery("GitHub", e, 1)
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
