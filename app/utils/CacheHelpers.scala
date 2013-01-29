package utils

import play.api.cache.Cache
import play.api.Play.current
import play.api.libs.ws.Response
import play.api.libs.json._

trait CacheHelpers {
  def etagHeaderFor(url: String): Seq[(String, String)] = {
    Cache.getAs[String]("etag_" + url) map { etag =>
      Seq("If-None-Match" -> etag)
    } getOrElse Nil
  }

  def etagHeaderFrom(response: Response): Option[String] = response.header("ETag")

  def cachedResponseFrom(response: Response): Option[JsValue] = {
    if(response.status == 304) {
      for {
        etag <- etagHeaderFrom(response)
        cachedResponse <- Cache.getAs[JsValue]("response_" + etag)
      } yield {
        cachedResponse
      }
    } else None
  }

  def cachedResponseOrElse(url: String)(implicit response: Response): JsValue = {
    cachedResponseFrom(response) getOrElse {
      etagHeaderFrom(response).foreach { etag =>
        cacheResponse(url, etag, response.json)
      }
      response.json
    }
  }

  def cacheResponse(url: String, etag: String, response: JsValue) {
    Cache.set("etag_" + url, etag, Config.Cache.expiration)
    Cache.set("response_" + etag, response, Config.Cache.expiration)
  }
}
