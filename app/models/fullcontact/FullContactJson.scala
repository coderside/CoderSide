package models.fullcontact

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object FullContactJson {

  val readPhoto: Reads[FullContactPhoto] = Json.reads[FullContactPhoto]
  implicit val readPhotos: Reads[List[FullContactPhoto]] = list(readPhoto)
  val readProfile: Reads[FullContactProfile] = Json.reads[FullContactProfile]
  implicit val readProfiles: Reads[List[FullContactProfile]] = list(readProfile)
  val readFullContact: Reads[FullContact] = Json.reads[FullContact]
}
