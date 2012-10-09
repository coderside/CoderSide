package test

import com.ning.http.client.Realm.AuthScheme
import org.specs2.mutable._
import play.Logger
import play.api.libs.concurrent._
import play.api.libs.concurrent.execution.defaultContext
import models.github.{ GitHubAPI, GitHubUser }

class GitHubApiSpec extends Specification {
  "GitHub API implementations " should {
    "search user by full name" in {
      val users = GitHubAPI.searchByFullname("sébastien renault").await match {
        case Redeemed(users) => users
        case Thrown(e) => Nil
      }
      users.size > 0  must equalTo(true)
    }

    "Get repositories by user" in {
      val repos = GitHubAPI.repositories("srenault").await match {
        case Redeemed(repositories) => repositories
        case Thrown(e) => Nil
      }
      repos.size > 0  must equalTo(true)
    }
  }
}
