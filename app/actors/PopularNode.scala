package actors

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import reactivemongo.bson.BSONObjectID
import Messages._
import models.CoderGuy
import models.twitter.TwitterAPI
import models.popular.PopularCoder
import models.popular.PopularCoderJson

class PopularNode() extends Actor with ActorLogging {
  self =>

  def receive = {
    case UpdatePopular(coderGuy) => {
      val gathererRef = sender
      log.info("[PopularNode] Updating popular !")
      coderGuy.gitHubUser map { gitHub =>
        PopularCoder.findByPseudo(gitHub.login).collect {
          case Some(coderAsJson) => {
            PopularCoderJson.readPopularCoder.reads(coderAsJson).map(Some(_)).recoverTotal { error =>
              log.error("An error occured while reading popular coder: " + error)
              None
            }
          }
        }.collect { case Some(coder) => coder } map { coder =>
          coder.increment() map { pts =>
            gathererRef ! pts
            PopularCoder.generateTweet(coderGuy, pts) foreach { tweet =>
              TwitterAPI.updateStatuses(tweet)
            }
          } recover {  case e: Exception => gathererRef ! 1L }
        } recover {
          case e: NoSuchElementException => {
            log.error("An error occured while updating popular !")
            val popularCoder = PopularCoder(
              BSONObjectID.generate,
              gitHub.login,
              coderGuy.oneFullname,
              coderGuy.twitterUser flatMap (_.description),
              1
            )
            PopularCoder.uncheckedCreate(popularCoder)
            PopularCoder.generateTweet(coderGuy, 1) foreach { tweet =>
              TwitterAPI.updateStatuses(tweet)
            }
            gathererRef ! 1L
          }
          case e: Exception => {
            log.error("Error while saving popular coder info: " + e.getMessage)
            gathererRef ! 1L
          }
        }
      } getOrElse (gathererRef ! 1L)
    }
  }
}

object PopularNode {
  lazy val system = ActorSystem("PopularSystem")
  lazy val ref = system.actorOf(Props[PopularNode])
  def stop = system.stop(ref)
}
