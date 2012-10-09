package actors

import akka.actor.{ Actor, ActorSystem, Props, ActorRef, ActorLogging }
import scala.concurrent.util.duration._
import Messages._

class SupervisorNode extends Actor with ActorLogging {

  lazy val headNode = context.actorOf(Props[HeadNode])

  def receive = {
    case query @ InitQuery(gitHubUsers) => {
      log.debug("[NodeSupervisor] receiving an event")
      headNode ! HeadQuery(gitHubUsers, sender)
    }
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
