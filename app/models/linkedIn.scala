package models.linkedin

import scala.concurrent.Future
import scala.concurrent.future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.oauth.{ OAuthCalculator, ConsumerKey, RequestToken }
import utils.Config
import models.URLEncoder
import models.github.GitHubUser

object LinkedInAPI extends URLEncoder {

  val signatureCalc = OAuthCalculator(
    ConsumerKey(Config.linkedIn.key, Config.linkedIn.secretKey),
    RequestToken(Config.linkedIn.userToken, Config.linkedIn.userSecret)
  )

  implicit val readUser: Reads[LinkedInUser] = {
    (
      (__ \ 'id).read[String] and
      (__ \ 'firstName).read[String] and
      (__ \ 'lastName).read[String] and
      (__ \ 'headline).read[String] and
      (__ \ 'pictureUrl).read[String]
    )(LinkedInUser)
  }

  def searchByFullname(firstname: String, lastname: String): Future[List[LinkedInUser]] = {
    val uri = "http://api.linkedin.com/v1/people-search:(people:(headline,first-name,last-name,id,picture-url))"
    val params = "?first-name=%s&last-name=%s&sort=connections&format=json".format(encode(firstname), encode(lastname))

    WS.url(uri + params)
   .sign(signatureCalc)
   .get().map(_.json \ "people" \ "values") map {
       case JsArray(users) => users.flatMap { user =>
         readUser.reads(user).asOpt
       }.toList
       case _ => throw new LinkedInApiException("Failed seaching linkedIn user by fullname : " + firstname + " " + lastname)
    }
  }
}

object LinkedIn {
  def matchUser(gitHubUser: GitHubUser, linkedInUsers: List[LinkedInUser]): Option[LinkedInUser] = {
    val matchPseudo = (user: LinkedInUser) => gitHubUser.username.toLowerCase.trim == user.id.toLowerCase.trim
    val matchFullname = (user: LinkedInUser) => {
      val gitHubName = gitHubUser.fullname.toLowerCase.trim
      user.fullName.toLowerCase.trim ==  gitHubName ||
      user.fullName.split(" ").reverse.mkString(" ").toLowerCase.trim == gitHubName
    }

    val conditions = List(
      matchFullname -> 50,
      matchPseudo -> 50
    )

    def scoring(user: LinkedInUser, score: Int, tests: List[((LinkedInUser) => Boolean, Int)]): Int = {
      tests match {
        case Nil => score
        case (test, points) :: tail if test(user) => scoring(user, score + points, tail)
        case head :: tail => scoring(user, score, tail)
      }
    }

    linkedInUsers.map { linkedInUser =>
      linkedInUser -> scoring(linkedInUser, 0, conditions)
    }.sortBy (_._2).lastOption.map(_._1)
  }
}

case class LinkedInUser(id: String, firstName: String, lastName: String, headline: String, pictureUrl: String) {
  lazy val fullName = firstName + " " + lastName
}
case class LinkedInApiException(message: String) extends Exception
