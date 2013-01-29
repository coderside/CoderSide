package utils

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import reactivemongo.bson.BSONObjectID
import reactivemongo.core.commands.LastError

trait MongoHelpers {
  val writeObjectId: Reads[JsObject] = {
    def id = BSONObjectID.generate.stringify
      (__).json.pickBranch and
      (__ \ '_id).json.put(
        Json.obj(
          "$oid" -> JsString(id)
        )
      ) reduce
  }

  def handleLastError(lastError: LastError, success: Option[BSONObjectID], error: => String): Either[String, BSONObjectID] = {
    if(lastError.ok) {
      success map(Right(_)) getOrElse Left(error + " : ObjectId is none")
    } else Left(error)
  }

  def handleLastError(lastError: LastError, success: List[BSONObjectID], error: => String): Either[String, List[BSONObjectID]] = {
    if(lastError.ok) Right(success) else Left(error)
  }

  implicit object writeBSONObjectID extends Writes[BSONObjectID] {
    def writes(id: BSONObjectID): JsValue = JsString(id.stringify)
  }

  implicit object readBSONObjectID extends Reads[BSONObjectID] {
    def reads(idAsJson: JsValue): JsResult[BSONObjectID] = {
      idAsJson.asOpt[String] map { id =>
        JsSuccess(BSONObjectID(id))
      } getOrElse JsError("Failed to transform the JSON value into a BSONObjectID.")
    }
  }
}
