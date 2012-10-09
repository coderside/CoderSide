package test

import org.specs2.mutable._

import play.Logger
import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.concurrent._
import play.api.libs.concurrent.execution.defaultContext
import play.api.test._
import play.api.test.Helpers._
import models.klout.KloutAPI

class KloutApiSpec extends Specification {
  "Klout API implementations " should {
    "Get klout id by twitter id" in {
      running(FakeApplication()) {
        val kloutID = KloutAPI.kloutID("srenaultcontact").await match {
          case Redeemed(kloutID) => kloutID
          case Thrown(e) => None
        }
        kloutID.isDefined must equalTo(true)
      }
    }
    "Get klout topics by kout id" in {
      running(FakeApplication()) {
        val kloutID = KloutAPI.topics("585080").await match {
          case Redeemed(topics) => topics
          case Thrown(e) => Nil
        }
        kloutID.size > 0 must equalTo(true)
      }
    }
    "Get klout influence by kout id" in {
      running(FakeApplication()) {
        val influence = KloutAPI.influence("585080").await match {
          case Redeemed(influence) => Some(influence)
          case Thrown(e) => None
        }
        influence.isDefined must equalTo(true)
      }
    }
  }
}
