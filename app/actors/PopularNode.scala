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
      log.info("[PopularNode] Updating popular !")
      coderGuy.gitHubUser foreach { gitHub =>
        PopularCoder.findByPseudo(gitHub.login)
          .collect {
            case Some(coderAsJson) => {
              PopularCoderJson.readPopularCoder.reads(coderAsJson).map(Some(_)).recoverTotal { error =>
                log.error("An error occured while reading popular coder: " + error)
                None
              }
            }
          }
          .collect { case Some(coder) => coder } map { coder =>
             coder.increment() foreach { pts =>
               PopularCoder.generateTweet(coderGuy, pts) foreach { tweet =>
                 TwitterAPI.updateStatuses(tweet)
               }
             }
        } recover {
          case e: NoSuchElementException => {
            log.error("An error occured while updating popular !")
            val popularCoder = PopularCoder(
              BSONObjectID.generate,
              gitHub.login,
              coderGuy.oneFullname,
              coderGuy.twitterUser map(_.description),
              1
            )
            PopularCoder.uncheckedCreate(popularCoder)
            PopularCoder.generateTweet(coderGuy, 1) foreach { tweet =>
              TwitterAPI.updateStatuses(tweet)
            }
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
