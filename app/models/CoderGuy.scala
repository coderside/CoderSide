package models

import scala.concurrent.future
import scala.concurrent.Future
import play.api.mvc.RequestHeader
import play.api.libs.concurrent.Promise
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.Logger
import utils.Config
import models.popular.{ PopularCoder, PopularCoderJson }
import models.github._
import models.twitter._
import models.klout._

case class CoderGuy(
  gitHubUser: Option[GitHubUser],
  twitterUser: Option[TwitterUser],
  kloutUser: Option[KloutUser],
  errors: Seq[(String, String)] = Nil,
  viewed: Option[Long] = None
) {
  def profileURL(): Option[String] = {
    for {
      login <- gitHubUser map (_.login)
      fullname <- oneFullname
    } yield {
      Config.baseURL + "/#" + controllers.routes.Application.profile(login).url
    }
  }

  lazy val oneBio: Option[String] = {
    gitHubUser.flatMap(_.bio) filter(!_.trim.isEmpty) orElse
    twitterUser.flatMap(_.prettyDesc) filter(!_.trim.isEmpty)
  }

  lazy val oneFullname: Option[String] = {
    gitHubUser.flatMap(_.name) filter(!_.trim.isEmpty) orElse
    twitterUser.map(_.name) filter(!_.trim.isEmpty)
  }

  lazy val oneAvatar: Option[String] = {
    val maybeTwitterAvatar = for {
      twitter <- twitterUser
      avatar <- twitter.avatar
    } yield avatar

    val maybeGitHubAvatar = for {
      gitHub <- gitHubUser
      avatar <- gitHub.avatarUrl
      if(avatar != Config.gitHubDftAvatar)
        } yield avatar

    maybeGitHubAvatar orElse maybeTwitterAvatar
  }
}
