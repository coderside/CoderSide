package models.twitter

import scala.util.matching.Regex
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
import utils. { Config, CacheHelpers }
import models.{ URLEncoder, Debug }
import models.github.GitHubSearchedUser

object TwitterAPI extends URLEncoder with Debug with CacheHelpers {

  val signatureCalcSearch = OAuthCalculator(
    ConsumerKey(Config.Twitter.search.consumerKey, Config.Twitter.search.consumerSecret),
    RequestToken(Config.Twitter.search.accessToken, Config.Twitter.search.accessTokenSecret)
  )

  val signatureCalcPopular = OAuthCalculator(
    ConsumerKey(Config.Twitter.popular.consumerKey, Config.Twitter.popular.consumerSecret),
    RequestToken(Config.Twitter.popular.accessToken, Config.Twitter.popular.accessTokenSecret)
  )

  implicit val readUser: Reads[TwitterUser] = {
    (
      (__ \ 'screen_name).read[String] and
      (__ \ 'name).read[String] and
      (__ \ 'description).read[String] and
      (__ \ 'followers_count).read[Int] and
      (__ \ 'profile_image_url).readNullable[String]
    )((screenName, name, desc, followers, avatar) => TwitterUser(screenName, name, desc, followers, avatar))
  }

  implicit val readTweet = {
    (
      (__ \ 'text).read[String] and
      (__ \ 'created_at).read[String] and
      (__ \ 'retweeted).read[Boolean] and
      (__ \ 'in_reply_to_user_id).read[Option[String]] and
      (__ \ 'in_reply_to_status_id_str).readNullable[Option[String]]
    ) tupled
  }

  def searchBy(criteria: String): Future[List[TwitterUser]] = {
    val url = "https://api.twitter.com/1/users/search.json?q=" + encode(criteria)
    WS.url(url)
      .withHeaders(lastModifiedFor(url):_*)
      .sign(signatureCalcSearch)
      .get().map (implicit response => cachedResponseOrElse(url))
      .map {
        case users: JsArray => users.asOpt[List[TwitterUser]] getOrElse Nil
        case _ => throw new TwitterApiException("Failed seaching twitter user by : " + criteria)
      }
  }

  def show(twitterID: String): Future[Option[TwitterUser]] = {
    val url = "https://api.twitter.com/1/users/show.json"
    WS.url(url)
      .withHeaders(lastModifiedFor(url):_*)
      .withQueryString(
         "user_id" -> twitterID,
         "screen_name" -> twitterID
      )
      .sign(signatureCalcSearch)
      .get().map (implicit response => cachedResponseOrElse(url))
      .map { twitterUser =>
        twitterUser.asOpt[TwitterUser]
      }
  }

  def timeline(twitterID: String): Future[Option[TwitterTimeline]] = {
    val dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy")
    val id = encode(twitterID)
    val url = "https://api.twitter.com/1.1/statuses/user_timeline.json?user_id=%s&screen_name=%s".format(id, id)

    WS.url(url)
      .withHeaders(lastModifiedFor(url):_*)
      .sign(signatureCalcSearch)
      .get().map (implicit response => cachedResponseOrElse(url))
      .map {
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

  def updateStatuses(status: String) = {
    val url = "https://api.twitter.com/1.1/statuses/update.json?status=" + encode(status)
    WS.url(url)
      .sign(signatureCalcPopular)
      .post("")
      .map(_.json)
  }
}

case class TwitterTimeline(tweets: List[Tweet]) {
  lazy val retweets: List[Tweet] = tweets.filter(_.retweeted)
  lazy val pureTweets: List[Tweet] = tweets.filter(t => !t.inReplyToUser)
}

object Twitter {
  def matchUser(searchedUser: GitHubSearchedUser, twitterUsers: List[TwitterUser]): Option[TwitterUser] = {
    val matchPseudo = (user: TwitterUser)      => user.screenName.toLowerCase.trim == searchedUser.login.toLowerCase.trim
    val matchPseudoPart = (user: TwitterUser)  => user.screenName.toLowerCase.trim.contains(searchedUser.login.toLowerCase.trim)
    def containsLanguage = (user: TwitterUser) => user.description.toLowerCase.contains(searchedUser.language)
    def containsGitHub = (user: TwitterUser)   => user.description.toLowerCase.contains("github")
    val matchFullname = (user: TwitterUser) => {
      searchedUser.fullname.filter { name =>
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

  def pretty(text: String) = prettyHash(prettyUsername(prettyLink(text)))

  def prettyLink(text: String) = {
    val reg = new Regex("""(?i)(https?|ftp|file|http):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|]""")
    reg.replaceAllIn(text, link => """<a href="%s">%s</a>""".format(link, link));
  }

  def prettyUsername(text: String) = {
    val reg = new Regex("""@(\w+)""", "username")
    reg.replaceAllIn(text, m => """<span class="tweet-link">@</span><a href="http://www.twitter.com/%s">%s</a>""".format(m group "username", m group "username"));
  }

  def prettyHash(text: String) = {
    val reg = new Regex("""#(\w+)""", "hash")
    reg.replaceAllIn(text, m => """<span class="tweet-link">#</span><a href="http://search.twitter.com/search?q=%s">%s</a>""".format("%23" + (m group "hash"), m group "hash"));
  }
}

case class Tweet(
  text: String,
  createdAt: Date,
  retweeted: Boolean,
  inReplyToUser: Boolean,
  inReplyToStatus: Boolean
) {
  def pretty = Twitter.pretty(text)
}

case class TwitterUser(
  screenName: String,
  name: String,
  description: String,
  followers: Int,
  avatar: Option[String],
  timeline: Option[TwitterTimeline] = None
) {
  import Twitter._
  def prettyDesc = Twitter.pretty(description)
}

case class TwitterApiException(message: String) extends Exception
