package models.github

import scala.concurrent.Future
import scala.concurrent.future
import java.util.Date
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.util.control.Exception._
import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import models.{ URLEncoder, Debug }
import utils.{ Config, CacheHelpers }

object GitHubAPI extends URLEncoder with CacheHelpers with Debug {

  implicit val readGitHubUser: Reads[GitHubUser] =
    (
      (__ \ 'login).read[String] and
      (__ \ 'html_url).read[String] and
      (__ \ 'hireable).read[Boolean] and
      (__ \ 'followers).read[Long] and
      (__ \ 'blog).readNullable[String] and
      (__ \ 'bio).readNullable[String] and
      (__ \ 'email).readNullable[String] and
      (__ \ 'name).readNullable[String] and
      (__ \ 'company).readNullable[String] and
      (__ \ 'avatar_url).readNullable[String] and
      (__ \ 'gravatar_id).readNullable[String] and
      (__ \ 'location).readNullable[String]
    )((login, url, hireable, followers, blog, bio, email, name, company, avatar, gravatar, location) =>
      GitHubUser(login, url, hireable, followers, blog, bio, email, name, company, avatar, gravatar, location))

  implicit val formatSearchedUser = Json.format[GitHubSearchedUser]

  implicit val readOrganization: Reads[GitHubOrg] =
    (
      (__ \ 'login).read[String] and
      (__ \ 'repos_url).read[String] and
      (__ \ 'avatar_url).readNullable[String] and
      (__ \ 'html_url).read[String]
    )((login, reposUrl, avatarUrl, url) => GitHubOrg(login, reposUrl, avatarUrl, url))

  implicit val readRepository: Reads[GitHubRepository] = {
    (
      (__ \ 'name).read[String] and
      (__ \ 'description).read[String] and
      (__ \ 'language).readNullable[String] and
      (__ \ 'html_url).read[String] and
      (__ \ 'owner \ 'login).read[String] and
      (__ \ 'forks_count).read[Int] and
      (__ \ 'watchers_count).read[Int] and
      (__ \ 'fork).read[Boolean] and
      (__ \ 'updated_at).read[Date]
    ) ((name, desc, lang, htmlUrl, owner, forks, watchers, fork, updatedAt) =>
      GitHubRepository(name, desc, lang, htmlUrl, owner, forks, watchers, fork, updatedAt)
    )
  }

  val oauthURL = "client_id=" + Config.GitHub.clientID + "&client_secret=" + Config.GitHub.clientSecret

  def profile(username: String): Future[Option[GitHubUser]] = {
    val url = "https://api.github.com/users/" + encode(username)
    WS.url(url + "?" + oauthURL)
      .withHeaders(etagFor(url):_*)
      .get().map { implicit response =>
      cachedResponseOrElse(url).asOpt[GitHubUser]
    }
  }

  def searchByFullname(fullname: String): Future[List[GitHubSearchedUser]] = {
    val url = "https://api.github.com/legacy/user/search/" + encode(fullname)
    WS.url(url + "?" + oauthURL)
      .withHeaders(etagFor(url):_*)
      .get().map { implicit response =>
      (cachedResponseOrElse(url) \ "users") match {
        case users: JsArray => users.asOpt[List[GitHubSearchedUser]] getOrElse Nil
        case o => throw new GitHubApiException("Failed seaching gitHub user by fullname : " + fullname)
      }
    }
  }

  def repositoriesByUser(username: String): Future[List[GitHubRepository]] = {
    val url = "https://api.github.com/users/%s/repos".format(encode(username))
    WS.url(url + "?" + oauthURL)
      .withHeaders(lastModifiedFor(url):_*)
      .get().map { implicit response =>
      cachedResponseOrElse(url) match {
        case repos: JsArray => repos.asOpt[List[GitHubRepository]] getOrElse Nil
        case r => throw new GitHubApiException("Failed getting repositories for : " + username)
      }
    }
  }

  def repositoriesByOrg(org: String): Future[List[GitHubRepository]] = {
    val url = "https://api.github.com/orgs/%s/repos".format(encode(org))
    WS.url(url + "?" + oauthURL)
      .withHeaders(lastModifiedFor(url):_*)
      .get().map { implicit response =>
      (cachedResponseOrElse(url)) match {
        case repos: JsArray => repos.asOpt[List[GitHubRepository]] getOrElse Nil
        case r => throw new GitHubApiException("Failed getting repositories for : " + org)
      }
    }
  }

  def organizations(username: String): Future[List[GitHubOrg]] = {
    val url = "https://api.github.com/users/%s/orgs".format(encode(username))
    WS.url(url + "?" + oauthURL)
      .withHeaders(lastModifiedFor(url):_*)
      .get().map { implicit response =>
      (cachedResponseOrElse(url)) match {
        case orgs: JsArray => orgs.asOpt[List[GitHubOrg]] getOrElse Nil
        case r => throw new GitHubApiException("Failed getting organizations for : " + username)
      }
    }
  }

  def contributions(username: String, repository: GitHubRepository): Future[Long] = {
    val url = "https://api.github.com/repos/%s/%s/contributors".format(encode(repository.owner), encode(repository.name))
    WS.url(url + "?" + oauthURL)
      .withHeaders(lastModifiedFor(url):_*)
      .get().map { implicit response =>
      (cachedResponseOrElse(url)) match {
        case JsArray(contributors) => {
          contributors.map { contributor =>
            (contributor \ "login").asOpt[String] -> ((contributor \ "contributions").asOpt[Long] getOrElse 0L)
          }.toList.collect {
            case (Some(login), commits) if (username == login) => commits
          }.headOption getOrElse 0
        }
        case r => throw new GitHubApiException("Failed getting contributions for : " + username)
      }
    }
  }
}

case class GitHubApiException(message: String) extends Exception

case class GitHubUser(
  login: String,
  htmlUrl: String,
  hireable: Boolean,
  followers: Long,
  blog: Option[String] = None,
  bio: Option[String] = None,
  email: Option[String] = None,
  name: Option[String] = None,
  company: Option[String] = None,
  avatarUrl: Option[String] = None,
  gravatarId: Option[String] = None,
  location: Option[String] = None,
  language: Option[String] = None,
  repositories: List[GitHubRepository] = Nil,
  organizations: List[GitHubOrg] = Nil
)

case class GitHubSearchedUser(
  login: String,
  fullname: Option[String],
  language: Option[String] = None,
  followers: Option[Int] = None,
  location: Option[String] = None,
  reposCount: Option[Int] = None
) {

  private def escapeSpecialCaracters(fullname: String): String = {
    val specialCaracters = """[^\w \tÀÂÇÈÉÊËÎÔÙÛàâçèéêëîôùû-]""".r
    specialCaracters.replaceAllIn(fullname, "").trim
  }

  val firstname: Option[String] = fullname.flatMap { name =>
    val str = escapeSpecialCaracters(name).split(" ")
    if(str.size > 1) Some(str(0)) else None
  }

  val lastname: Option[String] = fullname.flatMap { name =>
    val str = escapeSpecialCaracters(name).split(" ")
    if(str.size > 1) Some(str.tail.mkString) else None
  }

  def isFullnameOk: Boolean = firstname.isDefined && lastname.isDefined
}

case class GitHubRepository(
  name: String,
  description: String,
  language: Option[String],
  url: String,
  owner: String,
  forks: Int,
  watchers: Int,
  fork: Boolean,
  updatedAt: Date,
  contributions: Long = 0
)

case class GitHubOrg(
  login: String,
  reposUrl: String,
  avatarUrl: Option[String],
  url: String,
  repositories: List[GitHubRepository] = Nil
)
