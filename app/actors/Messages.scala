package actors

import akka.actor.ActorRef
import models.github.{ GitHubUser, GitHubRepository, GitHubOrg }
import models.twitter.{ TwitterUser, TwitterTimeline }
import models.klout.KloutUser
import models.linkedin.LinkedInUser

object Messages {
  //Node to Gatherer
  case class ErrorQuery(from: String, e: Throwable)
  //Application to Supervisor.
  case class InitQuery(profil: GitHubUser)
  //Supervisor to Head
  case class HeadQuery(gitHubUser: GitHubUser, client: ActorRef)
  //Head to Children
  case class NodeQuery(gitHubUser: GitHubUser, gatherer: ActorRef)
  //Head to Twitter Node
  case class TwitterNodeQuery(gitHubUser: GitHubUser, klout: ActorRef, gatherer: ActorRef)
  //Twitter Node to Klout Node
  case class KloutNodeQuery(twitterUser: TwitterUser, gatherer: ActorRef)
  //Klout to Klout
  case class KloutUserQuery(kloutUser: KloutUser, gathererRef: ActorRef)
  //GitHub to GitHub
  case class GitHubOrgQuery(gitHubUser: GitHubUser, gathererRef: ActorRef)
  //GitHub to GitHub
  case class GitHubContribQuery(gitHubUser: GitHubUser, gathererRef: ActorRef)
  //GitHub to Gatherer
  case class GitHubResult(profil: GitHubUser)
  //LinkedIn to Gatherer
  case class LinkedInResult(profil: LinkedInUser)
  //Klout to Gatherer
  case class KloutResult(profil: KloutUser)
  //Twitter to Gatherer
  case class TwitterResult(profil: TwitterUser)
  //Twitter to Twitter
  case class TwitterUserQuery(gitHubUser: GitHubUser, klout: ActorRef, gatherer: ActorRef)
  //Twitter to Twitter
  case class TwitterTimelineQuery(twitterUser: TwitterUser, gatherer: ActorRef)
  //For Gatherer only. Check if the gatering is complete
  object CheckResult
  //For Gatherer only. Decrement the rest of waited sub results
  object Decrement
  //Node to Gatherer
  case class NotFound(message: String)
  //Gatherer Node to client
  case class Progress(request: String)
  //Head Node to gatherer node
  object AskProgress
  //Supervisor Node to head node
  case class AskProgress(gitHubUser: GitHubUser)
  //Supervisor Node to head node
  case class NewClient(client: ActorRef)
  //Children to Head
  case class End(headNode: ActorRef)
}
