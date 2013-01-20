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
import models.{ URLEncoder, Debug }
import models.github.GitHubUser

object LinkedInAPI extends URLEncoder with Debug {

  val signatureCalc = OAuthCalculator(
    ConsumerKey(Config.LinkedIn.key, Config.LinkedIn.secretKey),
    RequestToken(Config.LinkedIn.userToken, Config.LinkedIn.userSecret)
  )

  implicit val readUser: Reads[LinkedInUser] = {
    (
      (__ \ 'id).read[String] and
      (__ \ 'firstName).read[String] and
      (__ \ 'lastName).read[String] and
      (__ \ 'headline).read[String] and
      (__ \ 'pictureUrl).readNullable[String]
    )(LinkedInUser)
  }

  def searchByFullname(firstname: String, lastname: String): Future[List[LinkedInUser]] = {
    val uri = "http://api.linkedin.com/v1/people-search:(people:(headline,first-name,last-name,id,picture-url))"
    val params = "?first-name=%s&last-name=%s&facet=industry,3,4,5,6,8,96,106,109,114,118&sort=relevance&format=json".format(encode(firstname), encode(lastname))

    WS.url(uri + params)
   .sign(signatureCalc)
   .get().map(debug).map(response => (response.json, response.json \ "people" \ "values")) map {
       case (_, JsArray(users)) => users.flatMap { user =>
         readUser.reads(user).asOpt
       }.toList
       case (response, _) =>
         if((response \ "people" \ "_total").asOpt[Int].filter(_ == 0).isDefined)
           Nil
         else
           throw new LinkedInApiException("Failed seaching linkedIn user by fullname : " + firstname + " " + lastname)
    }
  }
}

object LinkedIn {
  def matchUser(gitHubUser: GitHubUser, linkedInUsers: List[LinkedInUser]): Option[LinkedInUser] = {
    val matchFullname = (user: LinkedInUser) => {
      gitHubUser.fullname.filter { name =>
        val gitHubName = name.toLowerCase.trim
        user.fullName.toLowerCase.trim == gitHubName ||
        user.fullName.split(" ").reverse.mkString(" ").toLowerCase.trim == gitHubName
      }.isDefined
    }

    val conditions = List(
      matchFullname -> 50
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

case class LinkedInUser(id: String, firstName: String, lastName: String, headline: String, pictureUrl: Option[String]) {
  lazy val fullName = firstName + " " + lastName
}
case class LinkedInApiException(message: String) extends Exception
