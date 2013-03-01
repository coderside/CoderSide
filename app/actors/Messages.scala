package actors

import akka.actor.ActorRef
import models.github._
import models.twitter.{ TwitterUser, TwitterTimeline }
import models.klout.KloutUser
import models.CoderGuy

object Messages {
  //Node to Gatherer
  case class ErrorQuery(from: String, e: Throwable, notProcessed: Int)
  //Application to Supervisor.
  case class InitQuery(user: GitHubUser)
  //Supervisor to Head
  case class HeadQuery(user: GitHubUser, client: ActorRef)
  //Head to Children
  case class NodeQuery(user: GitHubUser, gatherer: ActorRef)
  //Head to Twitter Node
  case class TwitterNodeQuery(
    user: GitHubUser,
    klout: ActorRef,
    gatherer: ActorRef
  )
  //Twitter Node to Klout Node
  case class KloutNodeQuery(twitterUser: TwitterUser, gatherer: ActorRef)
  //Klout to Klout
  case class KloutUserQuery(kloutUser: KloutUser, gathererRef: ActorRef)
  //GitHub to GitHub
  case class GitHubOrgQuery(gitHubUser: GitHubUser, gathererRef: ActorRef)
  //GitHub to GitHub
  case class GitHubContribQuery(gitHubUser: GitHubUser, gathererRef: ActorRef)
  //GitHub to Gatherer
  case class GitHubResult(gitHubUser: GitHubUser)
  //Klout to Gatherer
  case class KloutResult(kloutUser: KloutUser)
  //Twitter to Gatherer
  case class TwitterResult(twitterUser: TwitterUser)
  //Twitter to Twitter
  case class TwitterUserQuery(
    user: GitHubUser,
    klout: ActorRef,
    gatherer: ActorRef
  )
  //Twitter to Twitter
  case class TwitterTimelineQuery(twitterUser: TwitterUser, gatherer: ActorRef)
  //For Gatherer only. Check if the gatering is complete
  object CheckResult
  //For Gatherer only. Decrement the rest of waited sub results
  case class Decrement(nb: Int = 1)
  //Node to Gatherer
  case class NotFound(message: String, notProcessed: Int)
  //Gatherer Node to client
  case class Progress(request: String)
  //Head Node to gatherer node
  case class AskProgress(username: String)
  //Supervisor Node to head node
  case class NewClient(client: ActorRef)
  //Children to Head
  case class End(headNode: ActorRef)
  //Gatherer Node to Popular node
  case class UpdatePopular(coderGuy: CoderGuy)
}
