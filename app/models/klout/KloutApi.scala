package models.klout

import scala.concurrent.Future
import scala.concurrent.future
import scala.util.control.Exception._
import play.Logger
import play.api.libs.ws._
import play.api.libs.json.JsArray
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.twitter.TwitterUser
import utils.Config
import models.{ URLEncoder, Debug }

object KloutAPI extends URLEncoder with Debug {

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
          KloutJson.readUser.reads(json).asOpt
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
        val influencersAsJson = JsArray(influence \ "myInfluencers" \\ "entity")
        val influencers = KloutJson.readInfluences.reads(influencersAsJson).recoverTotal { error =>
          Logger.error("An error occurred while reading klout influencers: " + error.toString)
          Nil
        }
        val influenceesAsJson = JsArray(influence \ "myInfluencees" \\ "entity")
        val influencees = KloutJson.readInfluences.reads(influenceesAsJson).recoverTotal { error =>
          Logger.error("An error occurred while reading klout influencees: " + error.toString)
          Nil
        }
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
