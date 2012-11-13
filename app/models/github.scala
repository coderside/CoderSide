package models.github

import scala.concurrent.Future
import scala.concurrent.future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.json.util._
import models.URLEncoder

object GitHubAPI extends URLEncoder {

  implicit val readUser: Reads[GitHubUser] = {
    (
      (__ \ 'username).read[String] and
      (__ \ 'fullname).read[String] and
      (__ \ 'language).read[String] and
      (__ \ 'followers).read[Int]
    )(GitHubUser)
  }

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

  implicit val readRepository: Reads[GitHubRepository] = {
    (
      (__ \ 'name).read[String] and
      (__ \ 'description).read[String] and
      (__ \ 'language).read[String] and
      (__ \ 'url).read[String] and
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

  def repositories(username: String): Future[List[GitHubRepository]] = {
    WS.url("https://api.github.com/users/%s/repos".format(encode(username)))
   .get().map(_.json).map {
     case JsArray(reps) => reps.flatMap { rep =>
       readRepository.reads(rep).asOpt
     }.toList
     case r => throw new GitHubApiException("Failed getting repositories for : " + username)
    }
  }
}

case class GitHubApiException(message: String) extends Exception

case class GitHubUser(username: String, fullname: String, language: String, followers: Int) {
  val firstname: String = {
    val str = fullname.split(" ")
    if(str.size == 2) str(0) else ""
  }

  val lastname: String = {
    val str = fullname.split(" ")
    if(str.size == 2) str(1) else ""
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
