package utils

import play.api.Logger
import play.api.Play
import play.api.Play.current
import scala.concurrent.duration._

object Config {

  lazy val baseURL: String = {
    Play.configuration.getString("baseURL") getOrElse {
      Logger.warn("[Config] Please provide base URL")
      throw new ConfigException("Please provide base URL")
    }
  }

  lazy val gathererWaited: Double = {
    Play.configuration.getDouble("gatherer.waited") getOrElse {
      Logger.warn("[Config] Please provide GathererNode waited value")
      throw new ConfigException("Please provide GathererNode waited value")
    }
  }

  lazy val overviewTimeout: FiniteDuration = {
    Play.configuration.getInt("overview.timeout").map(number => number seconds).getOrElse {
      Logger.warn("[Config] Please provide overview timeout value")
      throw new ConfigException("Please provide overview timeout value")
    }
  }

  lazy val gathererTimeout: FiniteDuration = {
    Play.configuration.getInt("gatherer.timeout").map(number => number seconds).getOrElse {
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
    Play.configuration.getInt("supervisor.strategy.withinMinutes").map(number => number minutes).getOrElse {
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
    Play.configuration.getInt("head.strategy.withinMinutes").map(number => number minutes).getOrElse {
      Logger.warn("[Config] Please provide HeadNode strategy within value")
      throw new ConfigException("Please provide HeadNode supervisor strategy within value")
    }
  }

  object GitHub {
    lazy val clientID: String = {
      Play.configuration.getString("oauth.github.clientid").getOrElse {
        Logger.warn("[Config] Please provide github clientid")
        throw new ConfigException("Please provide GitHub client id")
      }
    }

    lazy val clientSecret: String = {
      Play.configuration.getString("oauth.github.clientsecret").getOrElse {
        Logger.warn("[Config] Please provide GitHub client secret")
        throw new ConfigException("Please provide GitHub client secret")
      }
    }
  }

  object LinkedIn {
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

  object Klout {
    lazy val key: String = {
      Play.configuration.getString("oauth.klout.key").getOrElse {
        Logger.warn("[Config] Please provide klout key")
        throw new ConfigException("Please provide klout key")
      }
    }
  }

  object Twitter {
    object search {
      lazy val consumerKey: String = {
        Play.configuration.getString("oauth.twitter.search.consumerkey").getOrElse {
          Logger.warn("[Config] Please provide twitter consummer key for search")
          throw new ConfigException("Please provide twitter consummer key for search")
        }
      }

      lazy val consumerSecret: String = {
        Play.configuration.getString("oauth.twitter.search.consumersecret").getOrElse {
          Logger.warn("[Config] Please provide twitter consummer secret for search")
          throw new ConfigException("Please provide twitter consummer secret for search")
        }
      }

      lazy val accessToken: String = {
        Play.configuration.getString("oauth.twitter.search.accesstoken").getOrElse {
          Logger.warn("[Config] Please provide twitter access token for search")
          throw new ConfigException("Please provide twitter access token for search")
        }
      }

      lazy val accessTokenSecret: String = {
        Play.configuration.getString("oauth.twitter.search.accesstokensecret").getOrElse {
          Logger.warn("[Config] Please provide twitter access token secret for search")
          throw new ConfigException("Please provide twitter access token secret for search")
        }
      }
    }

    object popular {
      lazy val consumerKey: String = {
        Play.configuration.getString("oauth.twitter.popular.consumerkey").getOrElse {
          Logger.warn("[Config] Please provide twitter consummer key for popular")
          throw new ConfigException("Please provide twitter consummer key for popular")
        }
      }

      lazy val consumerSecret: String = {
        Play.configuration.getString("oauth.twitter.popular.consumersecret").getOrElse {
          Logger.warn("[Config] Please provide twitter consummer secret for popular")
          throw new ConfigException("Please provide twitter consummer secret for popular")
        }
      }

      lazy val accessToken: String = {
        Play.configuration.getString("oauth.twitter.popular.accesstoken").getOrElse {
          Logger.warn("[Config] Please provide twitter access token for popular")
          throw new ConfigException("Please provide twitter access token for popular")
        }
      }

      lazy val accessTokenSecret: String = {
        Play.configuration.getString("oauth.twitter.popular.accesstokensecret").getOrElse {
          Logger.warn("[Config] Please provide twitter access token secret for popular")
          throw new ConfigException("Please provide twitter access token secret for popular")
        }
      }
    }
  }

  object Cache {
    lazy val expiration: Int = {
      Play.configuration.getInt("cache.expiration").getOrElse {
        Logger.warn("[Config] Please provide a cache expiration time")
        throw new ConfigException("Please provide a cache expiration cache")
      }
    }
  }
}

case class ConfigException(message: String) extends Exception
