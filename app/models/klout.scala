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
import models.{ URLEncoder, Debug }

object KloutAPI extends URLEncoder with Debug {

  implicit val readInfluence: Reads[KloutUser] = {
    (
      (__ \ 'payload \ 'kloutId).read[String] and
      (__ \ 'payload \ 'nick).read[String] and
      (__ \ 'payload \ 'score \ 'score).read[Double]
    )((id, nick, score) => KloutUser(id, nick, score))
  }

  val readUser: Reads[KloutUser] = {
    (
      (__ \ 'kloutId).read[String] and
      (__ \ 'nick).read[String] and
      (__ \ 'score \ 'score).read[Double]
    )((id, nick, score) => KloutUser(id, nick, score))
  }

  def kloutID(twitterID: String): Future[Option[String]] = {
    WS.url("http://api.klout.com/v2/identity.json/twitter")
      .withQueryString(
        "screenName" -> twitterID,
        "key" -> Config.Klout.key
      )
      .get().map { response =>
        catching(classOf[Exception]).opt(response.json).flatMap { json =>
          (json \ "id").asOpt[String]
        }
      }
  }

  def kloutUser(kloutID: String): Future[Option[KloutUser]] = {
    WS.url("http://api.klout.com/v2/user.json/" + encode(kloutID))
      .withQueryString("key" -> Config.Klout.key)
      .get().map { response =>
        catching(classOf[Exception]).opt(response.json).flatMap { json =>
          readUser.reads(json).asOpt
        }
      }
  }

  def topics(kloutID: String): Future[List[String]] = {
    val uri = "http://api.klout.com/v2/user.json/%s/topics".format(encode(kloutID))
    WS.url(uri)
      .withQueryString("key" -> Config.Klout.key)
      .get().map { response =>
        (response.json \\ "displayName").flatMap(topic => topic.asOpt[String]).toList
    }
  }

  def influence(kloutID: String): Future[Influence] = {
    val uri = "http://api.klout.com/v2/user.json/%s/influence".format(encode(kloutID))
    WS.url(uri)
      .withQueryString("key" -> Config.Klout.key)
      .get().map(_.json).map { influence =>
        val influencers = JsArray(influence \ "myInfluencers" \\ "entity").as[List[KloutUser]]
        val influencees = JsArray(influence \ "myInfluencees" \\ "entity").as[List[KloutUser]]
        Influence(influencers, influencees)
    }
  }
}

object Klout {
  type KloutId = String

  def flattenTwitterUsers(twitterUsers: List[(KloutId, Option[TwitterUser])]) =
    twitterUsers collect {
      case (kloutId, Some(twitterUser)) => (kloutId, twitterUser)
    }

  def splitInfluence(influencers: List[KloutUser], influencees: List[KloutUser], twitterUsers: List[(KloutId, TwitterUser)]) = {
    val (twInfluencers, twInfluencees) = twitterUsers partition { case (kloutId, _) =>
      influencers find (_.id == kloutId) isDefined
    }

    def zipWithKloutUser(twitterUsers: List[(KloutId, TwitterUser)]): List[(KloutUser, TwitterUser)] = {
      twitterUsers map { case (kloutId, twitterUser) =>
        val kloutUser = (influencers ++ influencees) find { kloutUser =>
          kloutUser.id == kloutId
        }
        kloutUser -> twitterUser
      } collect {
        case (Some(kloutUser), twitter) => (kloutUser, twitter)
      }
    }
    zipWithKloutUser(twInfluencers) -> zipWithKloutUser(twInfluencees)
  }
}

case class KloutUser(
  id: String,
  nick: String,
  score: Double,
  influencers: List[(KloutUser, TwitterUser)] = Nil,
  influencees: List[(KloutUser, TwitterUser)] = Nil
)
case class Influence(influencers: List[KloutUser], influencees: List[KloutUser])
case class KloutApiException(message: String) extends Exception
