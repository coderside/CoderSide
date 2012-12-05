package models.twitter

import scala.concurrent.Future
import scala.concurrent.future
import java.util.Date
import java.text.SimpleDateFormat
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.oauth.{ OAuthCalculator, ConsumerKey, RequestToken }
import utils.Config
import models.{ URLEncoder, Debug }
import models.github.GitHubUser

object TwitterAPI extends URLEncoder with Debug {

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
    )((screenName, name, desc, followers) => TwitterUser(screenName, name, desc, followers))
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

  def searchBy(criteria: String): Future[List[TwitterUser]] = {
    WS.url("https://api.twitter.com/1/users/search.json?q=" + encode(criteria))
      .sign(signatureCalc)
      .get().map(_.json).map {
      case JsArray(users) => users.flatMap { user =>
        readUser.reads(user).asOpt
      }.toList
      case _ => throw new TwitterApiException("Failed seaching twitter user by : " + criteria)
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
    val id = encode(twitterID)
    val uri = "https://api.twitter.com/1.1/statuses/user_timeline.json?user_id=%s&screen_name=%s".format(id, id)

    WS.url(uri)
    .sign(signatureCalc)
    .get().map(_.json).map {
      case JsArray(tweets) => Some(TwitterTimeline(
        tweets.flatMap { tweetOpt =>
          readTweet.reads(tweetOpt).asOpt.map {
            case (text, createdAt, retweeted, inReplyToStatus, inReplyToUser) =>
              Tweet(text, dateFormat.parse(createdAt), retweeted, inReplyToStatus.isDefined, inReplyToUser.isDefined)
          }
        }.toList
      ))
      case _ => None
    }
  }
}

case class TwitterTimeline(tweets: List[Tweet]) {
  lazy val retweets: List[Tweet] = tweets.filter(_.retweeted)
  lazy val pureTweets: List[Tweet] = tweets.filter(t => !t.inReplyToUser)
}

object Twitter {
  def matchUser(gitHubUser: GitHubUser, twitterUsers: List[TwitterUser]): Option[TwitterUser] = {
    val matchPseudo = (user: TwitterUser)      => user.screenName.toLowerCase.trim == gitHubUser.username.toLowerCase.trim
    val matchPseudoPart = (user: TwitterUser)  => user.screenName.toLowerCase.trim.contains(gitHubUser.username.toLowerCase.trim)
    def containsLanguage = (user: TwitterUser) => user.description.toLowerCase.contains(gitHubUser.language)
    def containsGitHub = (user: TwitterUser)   => user.description.toLowerCase.contains("github")
    val matchFullname = (user: TwitterUser) => {
      gitHubUser.fullname.filter { name =>
        val gitHubName = name.toLowerCase.trim
        user.name.toLowerCase.trim ==  gitHubName ||
        user.name.split(" ").reverse.mkString(" ").toLowerCase.trim == gitHubName
      }.isDefined
    }

    val conditions = List(
      matchFullname    -> 40,
      matchPseudo      -> 40,
      matchPseudoPart  -> 20,
      containsLanguage -> 10,
      containsGitHub   -> 10
    )

    def scoring(user: TwitterUser, score: Int, tests: List[((TwitterUser) => Boolean, Int)]): Int = {
      tests match {
        case Nil => score
        case (test, points) :: tail if test(user) => scoring(user, score + points, tail)
        case head :: tail => scoring(user, score, tail)
      }
    }

    twitterUsers.map { twitterUser =>
      twitterUser -> scoring(twitterUser, 0, conditions)
    }.sortBy (_._2).lastOption.map(_._1)
  }
}

case class Tweet(text: String, createdAt: Date, retweeted: Boolean, inReplyToUser: Boolean, inReplyToStatus: Boolean)
case class TwitterUser(
  screenName: String,
  name: String,
  description: String,
  followers: Int,
  timeline: Option[TwitterTimeline] = None
)
case class TwitterApiException(message: String) extends Exception
