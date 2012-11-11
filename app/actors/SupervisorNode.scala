package actors

import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import scala.util.{ Success, Failure }
import akka.actor.{ Actor, ActorSystem, Props, ActorRef, ActorLogging }
import akka.util.Timeout
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumerator
import Messages._

class SupervisorNode extends Actor with ActorLogging {

  lazy val headNode = context.actorOf(Props[HeadNode])

  def receive = {
    case query @ InitQuery(gitHubUser) => {
      log.debug("[NodeSupervisor] receiving an event")
      headNode ! HeadQuery(gitHubUser, sender)
    }
    case askProgress: AskProgress => headNode forward askProgress
  }

  override def preStart() = {
    log.debug("[NodeSupervisor] before starting...")
  }

  override def postStop() = {
    log.debug("[NodeSupervisor] after stopping...")
  }
}

object SupervisorNode {
  lazy val system = ActorSystem("SearchSystem")
  lazy val ref = system.actorOf(Props[SupervisorNode])
  def stop = system.stop(ref)
}
