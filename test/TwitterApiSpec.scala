package test

import org.specs2.mutable._

import play.Logger
import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.concurrent._
import play.api.libs.concurrent.execution.defaultContext
import play.api.test._
import play.api.test.Helpers._
import models.twitter.TwitterAPI

class TwitterApiSpec extends Specification {
  "Twitter API implementations " should {
    "search user by full name" in {
      running(FakeApplication()) {
        val users = TwitterAPI.searchByFullname("sébastien renault").await match {
          case Redeemed(users) => users
          case Thrown(e) => Nil
        }
        users.size > 0  must equalTo(true)
      }
    }

    "get user by twitter ID" in {
      running(FakeApplication()) {
        val user = TwitterAPI.show("srenaultcontact").await match {
          case Redeemed(user) => user
          case Thrown(e) => None
        }
        user.isDefined  must equalTo(true)
      }
    }
  }
}
