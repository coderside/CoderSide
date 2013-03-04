package models

import java.util.Date
import models.github._
import models.twitter._
import models.klout._

object Mock {

  val gitHubRepo = GitHubRepository(
    name = "CoderSide",
    description = "Having a full overview of one coder",
    language = Some("Scala"),
    url = "https://github.com/srenault/playstory",
    owner = "srenault",
    forks = 2,
    watchers = 3,
    fork = false,
    updatedAt = new Date,
    contributions = 10
  )

  val gitHubOrg = GitHubOrg(
    login = "Zenexity",
    reposUrl = "https://github.com/srenault/playstory",
    avatarUrl = Some("/assets/images/github/gravatar-user-420.png"),
    url = "https://github.com/srenault/playstory",
    repositories = List(gitHubRepo, gitHubRepo)
  )

  val gitHubUser = GitHubUser(
    login = "srenault",
    htmlUrl = "https://github.com/srenault",
    hireable = true,
    followers = 10,
    blog = Some("www.srenault.tumblr.com"),
    bio = Some("Web Architect @Zenexity"),
    email = Some("sre@zenexity.com"),
    name= Some("Sébastien RENAULT"),
    company = Some("Zenexity"),
    avatarUrl = Some("/assets/images/github/gravatar-user-420.png"),
    gravatarId = Some("1234456"),
    location = Some("Paris"),
    language = Some("Scala"),
    repositories = List(gitHubRepo, gitHubRepo, gitHubRepo),
    organizations = List(gitHubOrg, gitHubOrg, gitHubOrg)
  )

  val gitHubSearchedUser = GitHubSearchedUser(
    login = "srenault",
    fullname = Some("Sébastien RENAULT"),
    language = Some("Scala"),
    followers = Some(10),
    location = Some("Paris"),
    reposCount = Some(15)
  )

  val tweet = Tweet(
    text= "Awesome scala library ! @playframework Thx to all zenexity team !!",
    createdAt = new Date,
    retweeted = false,
    inReplyToUser = false ,
    inReplyToStatus = false
  )

  val tweets = List(tweet, tweet, tweet, tweet, tweet, tweet)

  val twitterTimeline = TwitterTimeline(tweets)

  val twitterUser = TwitterUser(
    screenName = "srenault",
    name = "Sébastien RENAULT",
    description = Some("Web developper @Zenexity"),
    followers = 10,
    avatar = None,
    timeline = Some(twitterTimeline)
  )

  val kUser = KloutUser(
    id = "23456",
    nick = "nrenault",
    score = 40.5,
    Nil,
    Nil
  )

  val kloutInfluencer = (kUser, twitterUser)

  val kloutInfluencers = List(kUser -> twitterUser)

  val kloutInfuencee = (kUser, twitterUser)

  val kloutInfluencees = List(kUser -> twitterUser)

  val kloutUser = KloutUser(
    id = "123456",
    nick = "srenault",
    score = 10,
    influencers = kloutInfluencers,
    influencees = kloutInfluencees
  )

  def searchedGitHubUser(): List[GitHubSearchedUser] = {
    List(gitHubSearchedUser, gitHubSearchedUser, gitHubSearchedUser, gitHubSearchedUser)
  }

  def coderGuy(): CoderGuy = {
    CoderGuy(
      Some(gitHubUser),
      Some(twitterUser),
      Some(kloutUser)
    )
  }
}
