package models.linkedin

import scala.concurrent.Future
import scala.concurrent.future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.json.util._
import play.api.libs.oauth.{ OAuthCalculator, ConsumerKey, RequestToken }
import utils.Config
import models.URLEncoder

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

  def searchByFullname(firstname: String, lastname: String): Future[Set[LinkedInUser]] = {

    val uri = "http://api.linkedin.com/v1/people-search:(people:(headline,first-name,last-name,id,picture-url))"
    val params = "?first-name=%s&last-name=%s&sort=connections&format=json".format(encode(firstname), encode(lastname))

    WS.url(uri + params)
   .sign(signatureCalc)
   .get().map(_.json \ "people" \ "values").map {
       case JsArray(users) => users.flatMap { user =>
         readUser.reads(user).asOpt
       }.toSet
       case _ => throw new LinkedInApiException("Failed seaching linkedIn user by fullname : " + firstname + " " + lastname)
    }
  }
}

case class LinkedInUser(id: String, firstName: String, lastName: String, headline: String, pictureUrl: String)
case class LinkedInApiException(message: String) extends Exception
