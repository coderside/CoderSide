package models

import models.github._
import models.twitter._
import models.linkedin._
import models.klout._

case class CoderGuy(
  organizations: List[GitHubOrg],
  repositories: List[GitHubRepository],
  linkedInUser: Option[LinkedInUser],
  twitterUser: Option[TwitterUser],
  twitterTimeline: Option[TwitterTimeline],
  kloutUser: Option[KloutUser],
  influencers: List[(KloutUser, TwitterUser)],
  influencees: List[(KloutUser, TwitterUser)]
) {
  val hasTwitterAccount = twitterUser.isDefined
  val hasLinkedInAccont = linkedInUser.isDefined
}

object CoderGuy {
  import play.api.libs.json._

  implicit val gitHubReposWrites = new Writes[GitHubRepository] {
    def writes(gr: GitHubRepository): JsValue = {
      Json.obj(
        "name" -> gr.name,
        "description" -> gr.description,
        "language" -> gr.language,
        "url" -> gr.url,
        "owner" -> gr.owner,
        "forks" -> gr.forks
      )
    }
  }

  implicit val gitHubOrgWrites = new Writes[GitHubOrg] {
    def writes(go: GitHubOrg): JsValue = {
      Json.obj(
        "login" -> go.login,
        "reposUrl" -> go.reposUrl,
        "avatarUrl" -> go.avatarUrl,
        "url" -> go.url
      )
    }
  }

  implicit val linkedInUserWrites = new Writes[LinkedInUser] {
    def writes(lu: LinkedInUser): JsValue = {
      Json.obj(
        "id" -> lu.id,
        "firstname" -> lu.firstName,
        "lastname" -> lu.lastName,
        "headline" -> lu.headline,
        "pictureUrl" -> lu.pictureUrl
      )
    }
  }

  implicit val twitterUserWrites = new Writes[TwitterUser] {
    def writes(tu: TwitterUser): JsValue = {
      Json.obj(
        "screenName" -> tu.screenName,
        "name" -> tu.name,
        "description" -> tu.description,
        "followers" -> tu.followers
      )
    }
  }

  implicit val tweetWrites = new Writes[Tweet] {
    def writes(t: Tweet): JsValue = {
      Json.obj(
        "text" -> t.text,
        "createdAt" -> t.createdAt.getTime,
        "retweeted" -> t.retweeted,
        "inReplyToUser" -> t.inReplyToUser,
        "inReplyToStatus" -> t.inReplyToStatus
      )
    }
  }

  implicit val twitterTimelineWrites = new Writes[TwitterTimeline] {
    def writes(tt: TwitterTimeline): JsValue = {
      Json.obj(
        "tweets" -> tt.tweets
      )
    }
  }

  implicit val kloutUserWrites = new Writes[KloutUser] {
    def writes(ku: KloutUser): JsValue = {
      Json.obj(
        "id" -> ku.id,
        "nick" -> ku.nick,
        "score" -> ku.score
      )
    }
  }

  implicit val coderGuyWrites = new Writes[CoderGuy] {
    def writes(cg: CoderGuy): JsValue = {
      val influencers = JsArray(
        cg.influencers map { case (kloutUser, twitterUser) =>
          Json.obj(
            "klout" -> kloutUser,
            "twitter" -> twitterUser
          )
        }
      )

      val influencees = JsArray(
        cg.influencees map { case (kloutUser, twitterUser) =>
          Json.obj(
            "klout" -> kloutUser,
            "twitter" -> twitterUser
          )
        }
      )

      Json.obj(
        "organizations"   -> cg.organizations,
        "repositories"    -> cg.repositories,
        "linkedInUser"    -> cg.linkedInUser,
        "twitterUser"     -> cg.twitterUser,
        "twitterTimeline" -> cg.twitterTimeline,
        "kloutUser"       -> cg.kloutUser,
        "influencers"     -> influencers,
        "influencees"     -> influencees
      )
    }
  }
}
