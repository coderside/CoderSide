package models.klout

import scala.concurrent.Future
import scala.concurrent.future
import scala.util.control.Exception._
import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.json.util._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.twitter.TwitterUser
import utils.Config
import models.URLEncoder

object KloutAPI extends URLEncoder {

  implicit val readUser: Reads[KloutUser] = {
    (
      (__ \ 'payload \ 'kloutId).read[String] and
      (__ \ 'payload \ 'nick).read[String] and
      (__ \ 'payload \ 'score \ 'score).read[Double]
    )(KloutUser)
  }

  def kloutID(twitterID: String): Future[Option[String]] = {
    WS.url("http://api.klout.com/v2/identity.json/twitter")
      .withQueryString(
        "screenName" -> twitterID,
        "key" -> Config.klout.key
      )
      .get().map { response =>
        catching(classOf[Exception]).opt(response.json).flatMap { json =>
          (json \ "id").asOpt[String]
        }
      }
  }

  def topics(kloutID: String): Future[Set[String]] = {
    val uri = "http://api.klout.com/v2/user.json/%s/topics".format(encode(kloutID))
    WS.url(uri)
      .withQueryString("key" -> Config.klout.key)
      .get().map { response =>
        (response.json \\ "displayName").flatMap(topic => topic.asOpt[String]).toSet
    }
  }

  def influence(kloutID: String): Future[Influence] = {
    val uri = "http://api.klout.com/v2/user.json/%s/influence".format(encode(kloutID))
    WS.url(uri)
      .withQueryString("key" -> Config.klout.key)
      .get().map(_.json).map { influence =>
        val influencers = (influence \ "myInfluencers" \\ "entity").flatMap { influencer =>
          readUser.reads(influencer).asOpt
        }.toSet
        val influencees = (influence \ "myInfluencees" \\ "entity").flatMap { influencee =>
          readUser.reads(influencee).asOpt
        }.toSet
        Influence(influencers, influencees)
    }
  }
}

case class KloutUser(id: String, nick: String, score: Double)
case class Influence(influencers: Set[KloutUser], influencees: Set[KloutUser])
case class KloutApiException(message: String) extends Exception
