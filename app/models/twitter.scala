package models.twitter

import scala.concurrent.Future
import scala.concurrent.future
import java.net.URLEncoder
import play.api.libs.concurrent.execution.defaultContext
import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.json.util._
import play.api.libs.oauth.{ OAuthCalculator, ConsumerKey, RequestToken }
import utils.Config

object TwitterAPI {

  lazy val signatureCalc = OAuthCalculator(
    ConsumerKey(Config.twitter.consumerKey, Config.twitter.consumerSecret),
    RequestToken(Config.twitter.accessToken, Config.twitter.accessTokenSecret)
  )

  val readUser: Reads[TwitterUser] = {
    (
      (__ \ 'name).read[String] and
      (__ \ 'description).read[String] and
      (__ \ 'followers).read[Int]
    )(TwitterUser)
  }

  def searchByFullname(fullname: String): Future[Set[TwitterUser]] = {
    WS.url("https://api.twitter.com/1/users/search.json?q=" + URLEncoder.encode(fullname, "UTF-8"))
   .sign(signatureCalc)
   .get().map(x.json).map {
       case JsArray(users) => users.flatMap { user =>
         readUser.reads(user).asOpt
       }.toSet
       case _ => throw new TwitterApiException("Failed seaching gitHub user by fullname : " + fullname)
    }
  }
}

case class TwitterApiException(message: String) extends Exception

case class Timeline(tweets: Set[Tweet] = Set.empty) {
  def retweets: Set[Tweet] = Set.empty
  def search(keywords: String*): Set[Tweet] = Set.empty
}

case class Tweet()
case class TwitterUser(name: String, description: String, followers: Int)
