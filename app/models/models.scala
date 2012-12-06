package models

import models.github._
import models.twitter._
import models.linkedin._
import models.klout._

case class CoderGuy(
  gitHubUser: Option[GitHubUser],
  linkedInUser: Option[LinkedInUser],
  twitterUser: Option[TwitterUser],
  kloutUser: Option[KloutUser]
)

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
        "forks" -> gr.forks,
        "watchers" -> gr.watchers,
        "fork" -> gr.fork,
        "updatedAt" -> gr.updatedAt,
        "contributions" -> gr.contributions
      )
    }
  }

  implicit val gitHubOrgWrites = new Writes[GitHubOrg] {
    def writes(go: GitHubOrg): JsValue = {
      Json.obj(
        "login" -> go.login,
        "reposUrl" -> go.reposUrl,
        "avatarUrl" -> go.avatarUrl,
        "url" -> go.url,
        "repositories" -> go.repositories
      )
    }
  }

  implicit val gitHubUserWrites = new Writes[GitHubUser] {
    def writes(gu: GitHubUser): JsValue = {
      Json.obj(
        "username" -> gu.username,
        "fullname" -> gu.fullname,
        "language" -> gu.language,
        "followers" -> gu.followers,
        "repositories" -> gu.repositories,
        "organizations" -> gu.organizations
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

  implicit val twitterUserWrites = new Writes[TwitterUser] {
    def writes(tu: TwitterUser): JsValue = {
      Json.obj(
        "screenName" -> tu.screenName,
        "name" -> tu.name,
        "description" -> tu.description,
        "followers" -> tu.followers,
        "timeline" -> tu.timeline
      )
    }
  }

  implicit val kloutUserWrites = new Writes[KloutUser] {

    def influenceJson(influence: List[(KloutUser, TwitterUser)]) = JsArray(
      influence map { case (kloutUser, twitterUser) =>
          Json.obj(
            "klout" -> Json.obj(
              "id" -> kloutUser.id,
              "nick" -> kloutUser.nick,
              "score" -> kloutUser.score
            ),
            "twitter" -> twitterUser
          )
      }
    )

    def writes(ku: KloutUser): JsValue = {
      Json.obj(
        "id" -> ku.id,
        "nick" -> ku.nick,
        "score" -> ku.score,
        "influencers" -> influenceJson(ku.influencers),
        "influencees" -> influenceJson(ku.influencees)
      )
    }
  }

  implicit val coderGuyWrites = new Writes[CoderGuy] {
    def writes(cg: CoderGuy): JsValue = {
      Json.obj(
        "gitHubUser"      -> cg.gitHubUser,
        "linkedInUser"    -> cg.linkedInUser,
        "twitterUser"     -> cg.twitterUser,
        "kloutUser"       -> cg.kloutUser
      )
    }
  }
}
