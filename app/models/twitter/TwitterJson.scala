package models.twitter

import java.util.Date
import play.api.libs.json._
import play.api.libs.json.Reads.list
import play.api.libs.functional.syntax._

object TwitterJson {

  val readUser: Reads[TwitterUser] = {
    (
      (__ \ 'screen_name).read[String] and
      (__ \ 'name).read[String] and
      (__ \ 'description).readNullable[String] and
      (__ \ 'followers_count).read[Int] and
      (__ \ 'profile_image_url).readNullable[String]
    )((screenName, name, desc, followers, avatar) => TwitterUser(screenName, name, desc, followers, avatar))
  }

  val readUsers: Reads[List[TwitterUser]] = list(readUser)

  val readTweet = {
    (
      (__ \ 'text).read[String] and
      (__ \ 'created_at).read[String] and
      (__ \ 'retweeted).read[Boolean] and
      (__ \ 'in_reply_to_user_id).read[Option[String]] and
      (__ \ 'in_reply_to_status_id_str).readNullable[Option[String]]
    ) tupled
  }
}
