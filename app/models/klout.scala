package models.klout

import scala.concurrent.Future
import scala.concurrent.future
import play.api.libs.concurrent.execution.defaultContext
import models.twitter.TwitterUser

object KloutAPI {
  def influence: Future[Influence] = future(Influence())
  def twitterID(kloutID: String): Future[TwitterUser] = future(TwitterUser())
}

case class Influence(influencers: Set[KloutUser] = Set.empty, influencees: Set[KloutUser] = Set.empty)
case class KloutUser()
