package models

import play.api.mvc.RequestHeader
import utils.Config
import models.github._
import models.twitter._
import models.linkedin._
import models.klout._

case class CoderGuy(
  gitHubUser: Option[GitHubUser],
  linkedInUser: Option[LinkedInUser],
  twitterUser: Option[TwitterUser],
  kloutUser: Option[KloutUser],
  errors: Seq[(String, String)] = Nil
) {
  def profileURL(): Option[String] = {
    for {
      login <- gitHubUser map (_.login)
      language <- gitHubUser flatMap (_.language)
      fullname <- oneFullname
    } yield {
      Config.baseURL + "/#" + controllers.routes.Application.profile(login, fullname, language).url
    }
  }

  lazy val oneFullname: Option[String] = {
    linkedInUser.map(_.fullName) orElse
    twitterUser.map(_.name) orElse
    gitHubUser.flatMap(_.name)
  }

  lazy val oneAvatar: Option[String] = {
    val linkedInAvatar = for {
      linkedIn <- linkedInUser
      avatar <- linkedIn.pictureUrl
    } yield avatar

    val twitterAvatar = for {
      twitter <- twitterUser
      avatar <- twitter.avatar
    } yield avatar
    linkedInAvatar orElse twitterAvatar
  }
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
        "username" -> gu.login,
        "fullname" -> gu.name,
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
    def errorsAsJson(errors: Seq[(String, String)]): JsValue = {
      JsArray(errors.map { case (who, what) =>
          Json.obj(
            "from" -> who,
            "what" -> what
          )
      })
    }

    def writes(cg: CoderGuy): JsValue = {
      Json.obj(
        "gitHubUser"      -> cg.gitHubUser,
        "linkedInUser"    -> cg.linkedInUser,
        "twitterUser"     -> cg.twitterUser,
        "kloutUser"       -> cg.kloutUser,
        "errors"          -> errorsAsJson(cg.errors)
      )
    }
  }
}
