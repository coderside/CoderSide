package actors

import akka.actor.{ Actor, ActorSystem, Props, ActorRef, ActorLogging }
import scala.concurrent.util.duration._

class SupervisorNode extends Actor with ActorLogging {
  import SupervisorNode.InitQuery
  import HeadNode.HeadQuery

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
  import models.github.GitHubUser

  case class InitQuery(profils: Set[GitHubUser])

  lazy val system = ActorSystem("SearchSystem")
  lazy val ref = system.actorOf(Props[SupervisorNode])

  def stop = system.stop(ref)
}
