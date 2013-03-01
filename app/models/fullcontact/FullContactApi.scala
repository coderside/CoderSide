package models.fullcontact

import scala.util.matching.Regex
import scala.concurrent.Future
import scala.concurrent.future
import play.Logger
import play.api.libs.json.JsArray
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws._
import utils.Config
import models.{ URLEncoder, Debug }
import models.twitter.TwitterUser
import models.twitter.TwitterAPI

object FullContactAPI extends URLEncoder with Debug {

  val apiKey = Config.fullcontact.key

  def searchByEmail(email: String): Future[Option[FullContact]] = {
    val url = "https://api.fullcontact.com/v2/person.json?email=%s&apiKey=%s".format(encode(email), apiKey)
    WS.url(url)
      .get().map(_.json)
      .map { response =>
      FullContactJson.readFullContact.reads(response).map(Some(_)).recoverTotal { error =>
        Logger.error("An error occured while getting social profile of " + email + " : " + error)
        None
      }
    }
  }
}

case class FullContactPhoto(
  url: String,
  `type`: String,
  typeId: String,
  typeName: String,
  isPrimary: Boolean
)

case class FullContactProfile(
  `type`: String,
  typeId: String,
  typeName: String,
  username: Option[String],
  id: Option[String],
  url: String
)

case class FullContact(
  photos: List[FullContactPhoto],
  socialProfiles: List[FullContactProfile]
) {
  def twitterProfile: Future[Option[TwitterUser]] = {
    socialProfiles.collectFirst {
      case profile: FullContactProfile if(profile.`type` == "twitter") => profile.username
    } collect { case Some(login) => login } map { login =>
      TwitterAPI.show(login)
    } getOrElse {
      Logger.warn("Can't find twitter profile in the social profile get from fullcontact")
      future(None)
    }
  }
}

case class FullContactApiException(message: String) extends Exception
