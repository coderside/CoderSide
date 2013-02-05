package actors

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import reactivemongo.bson.BSONObjectID
import Messages._
import models.{ PopularCoder, CoderGuy }
import models.twitter.TwitterAPI
import PopularCoder.json._

class PopularNode() extends Actor with ActorLogging {
  self =>

  def receive = {
    case UpdatePopular(coderGuy) => {
      coderGuy.gitHubUser foreach { gitHub =>
        PopularCoder.findByPseudo(gitHub.username)
          .collect { case Some(coderAsJson) => coderAsJson }
          .map (_.asOpt[PopularCoder])
          .collect { case Some(coder) => coder } map { coder =>
          PopularCoder.top(10) foreach { beforeAsJson =>
            val popularBefore = beforeAsJson.flatMap (_.asOpt[PopularCoder]) map(_.pseudo)
            val positionBefore = popularBefore indexOf(gitHub.username)
            coder.increment() foreach { pts =>
              PopularCoder.top(10) foreach { afterAsJson =>
                val popularAfter = afterAsJson flatMap (_.asOpt[PopularCoder]) map (_.pseudo)
                val positionAfter = popularAfter indexOf(gitHub.username)
                if(positionAfter < positionBefore) {
                  TwitterAPI.updateStatuses(PopularCoder.generateTweet(coderGuy, positionBefore - positionAfter, positionAfter))
                }
              }
            }
          }
        } recover {
          case e: NoSuchElementException => {
            val popularCoder = PopularCoder(
              BSONObjectID.generate,
              gitHub.username,
              coderGuy.oneFullname,
              coderGuy.twitterUser map(_.description),
              1,
              gitHub.language
            )
            PopularCoder.uncheckedCreate(popularCoder)
          }
          case e: Exception => log.error("Error while saving popular coder info: " + e.getMessage)
        }
      }
    }
  }
}

object PopularNode {
  lazy val system = ActorSystem("PopularSystem")
  lazy val ref = system.actorOf(Props[PopularNode])
  def stop = system.stop(ref)
}
