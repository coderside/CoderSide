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

  implicit val readUser: Reads[GitHubUser] =
    (
      (__ \ 'username).read[String] and
      (__ \ 'fullname).readNullable[String] and
      (__ \ 'language).readNullable[String] and
      (__ \ 'followers).read[Int] and
      (__ \ 'location).readNullable[String] and
      (__ \ 'repos).read[Int]
    )((username, fullname, language, followers, location, reposCount) =>
      GitHubUser(username, fullname, language, Some(followers), location, Some(reposCount))
    )

  implicit val gitHubUserWrites = new Writes[GitHubUser] {
    def writes(gu: GitHubUser): JsValue = {
      Json.obj(
        "username" -> gu.username,
        "fullname" -> gu.fullname,
        "language" -> gu.language,
        "followers" -> gu.followers
      )
    }
  }

  implicit val readOrganization: Reads[GitHubOrg] =
    (
      (__ \ 'login).read[String] and
      (__ \ 'repos_url).read[String] and
      (__ \ 'avatar_url).readNullable[String] and
      (__ \ 'url).read[String]
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

  def oauthURL = "client_id=" + Config.GitHub.clientID + "&client_secret=" + Config.GitHub.clientSecret

  def searchByFullname(fullname: String): Future[List[GitHubUser]] = {
    val url = "https://api.github.com/legacy/user/search/" + encode(fullname)
    WS.url(url + "?" + oauthURL)
      .withHeaders(etagFor(url):_*)
      .get().map { implicit response =>
      (cachedResponseOrElse(url) \ "users") match {
        case users: JsArray => users.asOpt[List[GitHubUser]] getOrElse Nil
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
  username: String,
  fullname: Option[String],
  language: Option[String],
  followers: Option[Int] = None,
  location: Option[String] = None,
  reposCount: Option[Int] = None,
  repositories: List[GitHubRepository] = Nil,
  organizations: List[GitHubOrg] = Nil
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
