package models.klout

import scala.concurrent.Future
import scala.concurrent.future
import scala.util.control.Exception._
import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.twitter.TwitterUser
import utils.Config
import models.URLEncoder

object KloutAPI extends URLEncoder {

  implicit val readInfluence: Reads[KloutUser] = {
    (
      (__ \ 'payload \ 'kloutId).read[String] and
      (__ \ 'payload \ 'nick).read[String] and
      (__ \ 'payload \ 'score \ 'score).read[Double]
    )(KloutUser)
  }

  implicit val readUser: Reads[KloutUser] = {
    (
      (__ \ 'kloutId).read[String] and
      (__ \ 'nick).read[String] and
      (__ \ 'score \ 'score).read[Double]
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

  def kloutUser(kloutID: String): Future[Option[KloutUser]] = {
    WS.url("http://api.klout.com/v2/user.json/" + encode(kloutID))
      .withQueryString("key" -> Config.klout.key)
      .get().map { response =>
        catching(classOf[Exception]).opt(response.json).flatMap { json =>
          readUser.reads(json).asOpt
        }
      }
  }

  def topics(kloutID: String): Future[List[String]] = {
    val uri = "http://api.klout.com/v2/user.json/%s/topics".format(encode(kloutID))
    WS.url(uri)
      .withQueryString("key" -> Config.klout.key)
      .get().map { response =>
        (response.json \\ "displayName").flatMap(topic => topic.asOpt[String]).toList
    }
  }

  def influence(kloutID: String): Future[Influence] = {
    val uri = "http://api.klout.com/v2/user.json/%s/influence".format(encode(kloutID))
    WS.url(uri)
      .withQueryString("key" -> Config.klout.key)
      .get().map(_.json).map { influence =>
        val influencers = (influence \ "myInfluencers" \\ "entity").flatMap { influencer =>
          readInfluence.reads(influencer).asOpt
        }.toList
        val influencees = (influence \ "myInfluencees" \\ "entity").flatMap { influencee =>
          readInfluence.reads(influencee).asOpt
        }.toList
        Influence(influencers, influencees)
    }
  }
}

case class KloutUser(id: String, nick: String, score: Double)
case class Influence(influencers: List[KloutUser], influencees: List[KloutUser])
case class KloutApiException(message: String) extends Exception
