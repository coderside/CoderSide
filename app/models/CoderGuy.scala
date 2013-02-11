package models

import play.api.mvc.RequestHeader
import utils.Config
import models.github._
import models.twitter._
import models.linkedin._
import models.klout._

case class CoderGuy(
  gitHubUser: Option[GitHubUser],
  twitterUser: Option[TwitterUser],
  kloutUser: Option[KloutUser],
  errors: Seq[(String, String)] = Nil
) {
  def profileURL(): Option[String] = {
    for {
      login <- gitHubUser map (_.login)
      fullname <- oneFullname
    } yield {
      Config.baseURL + "/#" + controllers.routes.Application.profile(login, fullname).url
    }
  }

  lazy val oneBio: Option[String] = {
    gitHubUser.flatMap(_.bio) orElse
    twitterUser.map(_.prettyDesc)
  }

  lazy val oneFullname: Option[String] = {
    gitHubUser.flatMap(_.name) orElse
    twitterUser.map(_.name)
  }

  lazy val oneAvatar: Option[String] = {
    val maybeTwitterAvatar = for {
      twitter <- twitterUser
      avatar <- twitter.avatar
    } yield avatar

    val maybeGitHubAvatar = for {
      gitHub <- gitHubUser
      avatar <- gitHub.avatarUrl
    } yield avatar
    maybeGitHubAvatar orElse maybeTwitterAvatar
  }
}
