package models.popular

import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.json.Reads.list
import play.api.libs.functional.syntax._
import reactivemongo.bson.BSONObjectID
import utils.MongoHelpers

object PopularCoderJson extends MongoHelpers {

  val writesPopularCoder: Writes[PopularCoder] = new Writes[PopularCoder] {
    def writes(coder: PopularCoder): JsValue = Json.obj(
      "_id" -> Json.obj("$oid" -> coder._id),
      "pseudo" -> coder.pseudo,
      "fullname" -> coder.fullname,
      "description" -> coder.description,
      "points" -> coder.points
    )
  }

  val readPopularCoder: Reads[PopularCoder] =
    (
      (__ \ '_id \ '$oid).read[BSONObjectID] and
      (__ \ 'pseudo).read[String] and
      (__ \ 'fullname).readNullable[String] and
      (__ \ 'description).readNullable[String] and
      (__ \ 'points).read[Long]
    )(PopularCoder)

  val readPopularCoders: Reads[List[PopularCoder]] = list(readPopularCoder)
}
