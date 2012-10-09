package models

import models.github.GitHubRepository
import models.twitter.{ TwitterUser, TwitterTimeline }
import models.linkedin.LinkedInUser

case class CoderGuy(
  repositories: Set[GitHubRepository],
  linkedInUser: Option[LinkedInUser],
  twitterUser: Option[TwitterUser],
  twitterTimeline: Option[TwitterTimeline],
  influencers: Set[TwitterUser],
  influencees: Set[TwitterUser]
) {
  val hasTwitterAccount = twitterUser.isDefined
  val hasLinkedInAccont = linkedInUser.isDefined
}
