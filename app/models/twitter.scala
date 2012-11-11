package models.twitter

import scala.concurrent.Future
import scala.concurrent.future
import java.net.URLEncoder
import java.util.Date
import java.text.SimpleDateFormat
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.json.util._
import play.api.libs.oauth.{ OAuthCalculator, ConsumerKey, RequestToken }
import utils.Config

object TwitterAPI {

  val signatureCalc = OAuthCalculator(
    ConsumerKey(Config.twitter.consumerKey, Config.twitter.consumerSecret),
    RequestToken(Config.twitter.accessToken, Config.twitter.accessTokenSecret)
  )

  implicit val readUser: Reads[TwitterUser] = {
    (
      (__ \ 'screen_name).read[String] and
      (__ \ 'name).read[String] and
      (__ \ 'description).read[String] and
      (__ \ 'followers_count).read[Int]
    )(TwitterUser)
  }

  implicit val readTweet = {
    (
      (__ \ 'text).read[String] and
      (__ \ 'created_at).read[String] and
      (__ \ 'retweeted).read[Boolean] and
      (__ \ 'in_reply_to_user_id).read[Option[String]] and
      (__ \ 'in_reply_to_status_id_str).readOpt[Option[String]]
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

  def timeline(twitterID: String): Future[Option[TwitterTimeline]] = {
    val dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy")
    val id = URLEncoder.encode(twitterID, "UTF-8")
    val uri = "https://api.twitter.com/1.1/statuses/user_timeline.json?user_id=%s&screen_name=%s"
              .format(id, id)

    WS.url(uri)
      .sign(signatureCalc)
      .get().map(_.json).map {
        case JsArray(tweets) => Some(TwitterTimeline(
          tweets.flatMap { tweetOpt =>
            readTweet.reads(tweetOpt).asOpt.map {
              case (text, createdAt, retweeted, inReplyToStatus, inReplyToUser) =>
                Tweet(text, dateFormat.parse(createdAt), retweeted, inReplyToStatus.isDefined, inReplyToUser.isDefined)
            }
          }.toSet
        ))
        case _ => None
      }
  }
}

case class TwitterTimeline(tweets: Set[Tweet]) {
  lazy val retweets: Set[Tweet] = tweets.filter(_.retweeted)
  lazy val pureTweets: Set[Tweet] = tweets.filter(t => !t.inReplyToUser)
}

case class Tweet(text: String, createdAt: Date, retweeted: Boolean, inReplyToUser: Boolean, inReplyToStatus: Boolean)
case class TwitterUser(screenName: String, name: String, description: String, followers: Int)
case class TwitterApiException(message: String) extends Exception
