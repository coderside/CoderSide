package utils

import play.api.Logger
import play.api.Play
import play.api.Play.current
import scala.concurrent.duration._

object Config {

  lazy val twitterBtnUrl: String = {
    Play.configuration.getString("twitter.btn.url") getOrElse {
      Logger.warn("[Config] Please provide twitter button URL")
      throw new ConfigException("Please provide twitter button URL")
    }
  }

  lazy val twitterBtnTweet: String = {
    Play.configuration.getString("twitter.btn.tweet") getOrElse {
      Logger.warn("[Config] Please provide twitter button tweet text")
      throw new ConfigException("Please provide twitter button tweet text")
    }
  }

  lazy val gathererWaited: Int = {
    Play.configuration.getInt("gatherer.waited") getOrElse {
      Logger.warn("[Config] Please provide GathererNode waited value")
      throw new ConfigException("Please provide GathererNode waited value")
    }
  }

  lazy val gathererTimeout: FiniteDuration = {
    Play.configuration.getInt("gatherer.timeout").map(minutes => 1 minutes).getOrElse {
      Logger.warn("[Config] Please provide GathererNode timeout value")
      throw new ConfigException("Please provide GathererNode timeout value")
    }
  }

  lazy val supervisorStrategyRetry: Int = {
    Play.configuration.getInt("supervisor.strategy.retry").getOrElse {
      Logger.warn("[Config] Please provide SupervisorNode strategy retries value")
      throw new ConfigException("Please provide supervisor strategy retries value")
    }
  }

  lazy val supervisorStrategyWithin: FiniteDuration = {
    Play.configuration.getInt("supervisor.strategy.withinMinutes").map(minutes => 1 minutes).getOrElse {
      Logger.warn("[Config] Please provide SupervisorNode strategy within value")
      throw new ConfigException("Please provide SupervisorNode supervisor strategy within value")
    }
  }

  lazy val headStrategyRetry: Int = {
    Play.configuration.getInt("head.strategy.retry").getOrElse {
      Logger.warn("[Config] Please provide HeadNode strategy retries value")
      throw new ConfigException("Please provide HeadNode supervisor strategy retries value")
    }
  }

  lazy val headStrategyWithin: FiniteDuration = {
    Play.configuration.getInt("head.strategy.withinMinutes").map(minutes => 1 minutes).getOrElse {
      Logger.warn("[Config] Please provide HeadNode strategy within value")
      throw new ConfigException("Please provide HeadNode supervisor strategy within value")
    }
  }

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
