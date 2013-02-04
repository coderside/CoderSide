package models

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo._
import play.modules.reactivemongo.PlayBsonImplicits._
import reactivemongo.api.SortOrder.{ Ascending, Descending }
import reactivemongo.api.{ QueryBuilder, QueryOpts }
import reactivemongo.core.commands.{ FindAndModify, Update }
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import reactivemongo.bson._
import utils.MongoHelpers

case class PopularCoder(
  _id: BSONObjectID,
  pseudo: String,
  fullname: Option[String],
  description: Option[String],
  points: Long = 1,
  language: Option[String] = None
) {
  def increment(): Future[Long] = {
    PopularCoder.increment(_id) collect { case Some(points) => points }
  }
}

object PopularCoder extends MongoHelpers with Function6[BSONObjectID,String, Option[String], Option[String], Long, Option[String], PopularCoder]{
  import json._

  val collectionName = "popular"
  val collection = ReactiveMongoPlugin.collection(collectionName)

  def increment(id: BSONObjectID): Future[Option[Long]] = {
    ReactiveMongoPlugin.db.command(
      FindAndModify(
        collection = collectionName,
        query = BSONDocument("_id" -> id),
        modify = Update(BSONDocument("$inc" -> BSONDocument("points" -> BSONInteger(1))), true)
      )
    ) map { maybeDoc =>
      maybeDoc flatMap (_.getAs[BSONLong]("points") map (_.value))
    }
  }

  def uncheckedCreate(coder: PopularCoder) {
    val coderAsJson = Json.toJson(coder)
    collection.uncheckedInsert(coderAsJson)
  }

  def findByPseudo(pseudo: String): Future[Option[JsValue]] = {
    val byPseudo = Json.obj("pseudo" -> pseudo)
    collection.find[JsValue, JsValue](byPseudo).headOption
  }

  def top(limit: Int): Future[List[JsValue]] = {
    val all = Json.obj()
    val sorted = QueryBuilder().query(all).sort("points" -> Descending)
    collection.find[JsValue](sorted, QueryOpts()).toList(limit)
  }

  object json {
    implicit val writesPopularCoder: Writes[PopularCoder] = new Writes[PopularCoder] {
      def writes(coder: PopularCoder): JsValue = Json.obj(
        "_id" -> Json.obj("$oid" -> coder._id),
        "pseudo" -> coder.pseudo,
        "fullname" -> coder.fullname,
        "description" -> coder.description,
        "points" -> coder.points,
        "language" -> coder.language
      )
    }

    implicit val readPopularCoder = 
    (
      (__ \ '_id \ '$oid).read[BSONObjectID] and
      (__ \ 'pseudo).read[String] and
      (__ \ 'fullname).readNullable[String] and
      (__ \ 'description).readNullable[String] and
      (__ \ 'points).read[Long] and
      (__ \ 'language).readNullable[String]
    )(PopularCoder)
  }
}