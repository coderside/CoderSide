package models.github

import scala.concurrent.Future
import scala.concurrent.future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.util.control.Exception._
import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import models.{ URLEncoder, Debug }

object GitHubAPI extends URLEncoder with Debug {

  private def handleJsNull(json: JsValue) = json match {
    case JsNull => None
    case json => json.asOpt[String]
  }

  implicit val readUser: Reads[GitHubUser] =
    (
      (__ \ 'username).read[String] and
      (__ \ 'fullname).json.pick.map(handleJsNull) and
      (__ \ 'language).json.pick.map(handleJsNull)and
      (__ \ 'followers).read[Int]
    )(GitHubUser)

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

  implicit val readOrganization: Reads[Organization] =
    (
      (__ \ 'login).read[String] and
      (__ \ 'repos_url).read[String] and
      (__ \ 'avatar_url).json.pick.map(handleJsNull) and
      (__ \ 'url).read[String]
    )(Organization)

  implicit val readRepository: Reads[GitHubRepository] = {
    (
      (__ \ 'name).read[String] and
      (__ \ 'description).read[String] and
      (__ \ 'language).read[String] and
      (__ \ 'html_url).read[String] and
      (__ \ 'owner \ 'login).read[String] and
      (__ \ 'forks_count).read[Int]
    )(GitHubRepository)
  }

  def searchByFullname(fullname: String): Future[Set[GitHubUser]] = {
    WS.url("https://api.github.com/legacy/user/search/" + encode(fullname))
   .get().map(_.json \ "users").map {
       case JsArray(users) => users.flatMap { user =>
         readUser.reads(user).asOpt
       }.toSet
       case o => throw new GitHubApiException("Failed seaching gitHub user by fullname : " + fullname)
    }
  }

  def repositoriesByUser(username: String): Future[List[GitHubRepository]] = {
    WS.url("https://api.github.com/users/%s/repos".format(encode(username)))
   .get().map(_.json).map {
     case JsArray(reps) => reps.flatMap { rep =>
       catching(classOf[Exception]).opt(
         readRepository.reads(rep).asOpt
       )
     }.flatten.toList
     case r => throw new GitHubApiException("Failed getting repositories for : " + username)
    }
  }

  def repositoriesByOrg(org: String): Future[List[GitHubRepository]] = {
    WS.url("https://api.github.com/orgs/%s/repos".format(encode(org)))
   .get().map(_.json).map {
     case JsArray(reps) => reps.flatMap { rep =>
       catching(classOf[Exception]).opt(
         readRepository.reads(rep).asOpt
       )
     }.flatten.toList
     case r => throw new GitHubApiException("Failed getting repositories for : " + org)
    }
  }

  def organizations(username: String): Future[List[Organization]] = {
    WS.url("https://api.github.com/users/%s/orgs".format(encode(username)))
   .get().map(_.json).map {
     case JsArray(orgs) => orgs.flatMap { org =>
       catching(classOf[Exception]).opt {
         readOrganization.reads(org).asOpt
       }
     }.flatten.toList
     case r => throw new GitHubApiException("Failed getting organizations for : " + username)
    }
  }
}

case class GitHubApiException(message: String) extends Exception

case class GitHubUser(username: String, fullname: Option[String], language: Option[String], followers: Int) {

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
}

case class GitHubRepository(
  name: String,
  description: String,
  language: String,
  url: String,
  owner: String,
  forks: Int
)

case class Organization(
  login: String,
  reposUrl: String,
  avatarUrl: Option[String],
  url: String
)
