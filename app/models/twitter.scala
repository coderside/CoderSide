package models.twitter

import scala.concurrent.Future
import scala.concurrent.future
import play.api.libs.concurrent.execution.defaultContext

trait TwitterAPI {
  def timeline: Future[Timeline]
  def people(userID: String): Future[TwitterUser]
}

object DftTwitterAPI extends TwitterAPI {
  def timeline: Future[Timeline] = future(Timeline())
  def people(userID: String): Future[TwitterUser] = future(TwitterUser())
}

case class Timeline(tweets: Set[Tweet] = Set.empty) {
  def retweets: Set[Tweet] = Set.empty
  def search(keywords: String*): Set[Tweet] = Set.empty
}

case class Tweet()
case class TwitterUser()
