package models.popular

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.api.Play.current
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo._
import play.modules.reactivemongo.PlayBsonImplicits._
import reactivemongo.api.SortOrder.{ Ascending, Descending }
import reactivemongo.api.{ QueryBuilder, QueryOpts }
import reactivemongo.core.commands.{ FindAndModify, Update }
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import reactivemongo.bson._
import models.CoderGuy

case class PopularCoder(
  _id: BSONObjectID,
  pseudo: String,
  fullname: Option[String],
  description: Option[String],
  points: Long = 1
) {
  def increment(): Future[Long] = {
    PopularCoder.increment(_id) collect { case Some(points) => points }
  }
}

object PopularCoder extends Function5[BSONObjectID,String, Option[String], Option[String], Long, PopularCoder]{
  val collectionName = "popular"
  val collection = ReactiveMongoPlugin.collection(collectionName)

  def generateTweet(coderGuy: CoderGuy, pts: Long): Option[String] = {
    val maybeTwitter = coderGuy.twitterUser map ("@" + _.screenName)
    val identification = maybeTwitter getOrElse coderGuy.gitHubUser.flatMap(_.name)
    coderGuy.profileURL.flatMap { url =>
      pts match {
        case pts: Long if(pts == 1) => Some(Messages("popular.twitter.first.visit", identification, url))
        case pts: Long if(pts % 10 == 0) => Some(Messages("popular.twitter.ten.visits", identification, url))
        case _ => None
      }
    }
  }

  def increment(id: BSONObjectID): Future[Option[Long]] = {
    ReactiveMongoPlugin.db.command(
      FindAndModify(
        collection = collectionName,
        query = BSONDocument("_id" -> id),
        modify = Update(BSONDocument("$inc" -> BSONDocument("points" -> BSONInteger(1))), true)
      )
    ) map { maybeDoc =>
      maybeDoc flatMap (_.getAs[BSONDouble]("points") map (_.value.toLong))
    }
  }

  def uncheckedCreate(coder: PopularCoder) {
    val coderAsJson = PopularCoderJson.writesPopularCoder.writes(coder)
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
}
