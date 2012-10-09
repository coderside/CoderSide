package utils

import play.api.Logger
import play.api.Play
import play.api.Play.current

object Config {

  object linkedIn {

    lazy val key: String = {
      Play.configuration.getString("oauth.linkedin.key").getOrElse {
        Logger.warn("[Config] Please provide LinkedIn key")
        throw new ConfigException("Please provide LinkedIn key")
      }
    }

    lazy val secretKey: String = {
      Play.configuration.getString("oauth.linkedin.secretkey").getOrElse {
        Logger.warn("[Config] Please provide LinkedIn secret key")
        throw new ConfigException("Please provide LinkedIn secret key")
      }
    }

    lazy val userToken: String = {
      Play.configuration.getString("oauth.linkedin.usertoken").getOrElse {
        Logger.warn("[Config] Please provide LinkedIn user token")
        throw new ConfigException("Please provide LinkedIn user token")
      }
    }

    lazy val userSecret: String = {
      Play.configuration.getString("oauth.linkedin.usersecret").getOrElse {
        Logger.warn("[Config] Please provide LinkedIn user secret")
        throw new ConfigException("Please provide LinkedIn user secret")
      }
    }
  }

  object klout {
    lazy val key: String = {
      Play.configuration.getString("oauth.klout.key").getOrElse {
        Logger.warn("[Config] Please provide klout key")
        throw new ConfigException("Please provide klout key")
      }
    }
  }

  object twitter {
    lazy val consumerKey: String = {
      Play.configuration.getString("oauth.twitter.consumerkey").getOrElse {
        Logger.warn("[Config] Please provide twitter consummer key")
        throw new ConfigException("Please provide twitter consummer key")
      }
    }

    lazy val consumerSecret: String = {
      Play.configuration.getString("oauth.twitter.consumersecret").getOrElse {
        Logger.warn("[Config] Please provide twitter consummer secret")
        throw new ConfigException("Please provide twitter consummer secret")
      }
    }

    lazy val accessToken: String = {
      Play.configuration.getString("oauth.twitter.accesstoken").getOrElse {
        Logger.warn("[Config] Please provide twitter access token")
        throw new ConfigException("Please provide twitter access token")
      }
    }

    lazy val accessTokenSecret: String = {
      Play.configuration.getString("oauth.twitter.accesstokensecret").getOrElse {
        Logger.warn("[Config] Please provide twitter access token secret")
        throw new ConfigException("Please provide twitter access token secret")
      }
    }
  }
}

case class ConfigException(message: String) extends Exception
