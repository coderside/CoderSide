package test

import org.specs2.mutable._

import play.Logger
import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.concurrent._
import play.api.libs.concurrent.execution.defaultContext
import play.api.test._
import play.api.test.Helpers._
import models.linkedin.LinkedInAPI

class LinkedInApiSpec extends Specification {
  "LinkedIn API implementations " should {
    "search user by full name" in {
      running(FakeApplication()) {
        val users = LinkedInAPI.searchByFullname("sébastien", "renault").await match {
          case Redeemed(users) => users
          case Thrown(e) => Nil
        }
        users.size > 0  must equalTo(true)
      }
    }
  }
}
