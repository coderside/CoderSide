package models.github

import scala.concurrent.Future
import scala.concurrent.future
import play.api.libs.concurrent.execution.defaultContext

object GitHubAPI {
  def searchBySkills(skills: String*): Future[Set[GitHubUser]] = future(Set(GitHubUser()))
  def repositories(userID: String): Future[Set[GitHubRepository]] = future(Set(GitHubRepository()))
}

case class GitHubUser()
case class GitHubRepository()
