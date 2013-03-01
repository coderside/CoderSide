package models.klout

import play.api.libs.json._
import play.api.libs.json.Reads.list
import play.api.libs.functional.syntax._

object KloutJson {

  val readInfluence: Reads[KloutUser] = {
    (
      (__ \ 'payload \ 'kloutId).read[String] and
      (__ \ 'payload \ 'nick).read[String] and
      (__ \ 'payload \ 'score \ 'score).read[Double]
    )((id, nick, score) => KloutUser(id, nick, score))
  }

  val readInfluences: Reads[List[KloutUser]] = list(readInfluence)

  val readUser: Reads[KloutUser] = {
    (
      (__ \ 'kloutId).read[String] and
      (__ \ 'nick).read[String] and
      (__ \ 'score \ 'score).read[Double]
    )((id, nick, score) => KloutUser(id, nick, score))
  }
}
