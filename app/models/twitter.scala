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
      (__ \ 'followers_count).read[Int]
    )(TwitterUser)
  }

  val readTweet = {
    (
      (__ \ 'text).read[String] and
      (__ \ 'url).read[String] and
      (__ \ 'description).read[String] and
      (__ \ 'retweeted).read[Boolean] and
      (__ \ 'in_reply_to_user_id).readOpt[String] and
      (__ \ 'in_reply_to_status_id_str).readOpt[String]
    ) tupled
  }

  def searchByFullname(fullname: String): Future[Set[TwitterUser]] = {
    WS.url("https://api.twitter.com/1/users/search.json?q=" + URLEncoder.encode(fullname, "UTF-8"))
   .sign(signatureCalc)
   .get().map(_.json).map {
       case JsArray(users) => users.flatMap { user =>
         readUser.reads(user).asOpt
       }.toSet
       case _ => throw new TwitterApiException("Failed seaching twitter user by fullname : " + fullname)
    }
  }

  def show(twitterID: String): Future[Option[TwitterUser]] = {
    WS.url("https://api.twitter.com/1/users/show.json")
   .withQueryString(
     "user_id" -> twitterID,
     "screen_name" -> twitterID
   )
   .sign(signatureCalc)
   .get().map(response => readUser.reads(response.json).asOpt)
  }

  def timeline(twitterID: String): Future[Timeline] = {
    WS.url("https://api.twitter.com/1.1/statuses/user_timeline.json")
      .withQueryString(
        "user_id" -> twitterID,
        "screen_name" -> twitterID
      )
      .sign(signatureCalc)
      .get().map(_.json).map {
        case JsArray(tweets) => Timeline(
          tweets.flatMap { tweetOpt =>
            readTweet.reads(tweetOpt).asOpt.map {
              case (text, url, description, retweeted, inReplyToStatus, inReplyToUser) =>
                Tweet(text, url, description, retweeted, inReplyToStatus.isDefined, inReplyToUser.isDefined)
            }
          }.toSet
        )
        case _ => throw new TwitterApiException("Failed getting tweets for user: " + twitterID)
      }
  }
}

case class Timeline(tweets: Set[Tweet])
case class Tweet(text: String, url: String, description: String, retweeted: Boolean, inReplyToUser: Boolean, inReplyToStatus: Boolean)
case class TwitterUser(name: String, description: String, followers: Int)
case class TwitterApiException(message: String) extends Exception
