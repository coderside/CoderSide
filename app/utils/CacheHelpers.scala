package utils

import java.util.TimeZone
import java.util.Date
import java.text.SimpleDateFormat
import play.api.cache.Cache
import play.api.Play.current
import play.api.Logger
import play.api.libs.ws.Response
import play.api.libs.json._

trait CacheHelpers {

  val KEY_LASTMODIFIED = "lastModified_"
  val KEY_ETAG = "etag_"
  val KEY_RESPONSE = "response_"

  def lastModifiedFormat = {
    val format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
    format.setTimeZone(TimeZone.getTimeZone("GMT"));
    format
  }

  def etagFrom(implicit response: Response): Option[String] = response.header("ETag")

  def lastModifiedFrom(implicit response: Response): Option[Date] = {
    response.header("Last-Modified") map { lastModified =>
      lastModifiedFormat.parse(lastModified)
    }
  }

  def etagResponse(url: String)(implicit response: Response): Option[JsValue] = {
    for {
      etag <- etagFrom(response)
      cachedResponse <- Cache.getAs[JsValue](KEY_RESPONSE + url)
    } yield {
      cachedResponse
    }
  }

  def lastModifiedResponse(url: String)(implicit response: Response): Option[JsValue] = {
    for {
      lastModified <- lastModifiedFrom(response)
      cachedResponse <- Cache.getAs[JsValue](KEY_RESPONSE + url)
    } yield {
      cachedResponse
    }
  }

  def cachedResponseFrom(
    url: String,
    newEtag: (String, JsValue) => Unit,
    newLastModified: (Date, JsValue) => Unit
  )(implicit response: Response): JsValue = {
    if(response.status == 304) {
      if(!etagResponse(url).isDefined && !lastModifiedResponse(url).isDefined) {
        Logger.debug("[CACHE] Not cached " + url)
      } else {
        Logger.debug("[CACHE] From the cache " + url)
      }
      (etagResponse(url) flatMap { etagResp =>
        lastModifiedResponse(url) orElse Some(etagResp)
      } orElse lastModifiedResponse(url)) getOrElse response.json
    } else {
      if(response.status == 200) {
        (etagFrom, lastModifiedFrom) match {
          case (_, Some(lastModified)) => {
            newLastModified(lastModified, response.json)
            response.json
          }
          case (Some(etag), None) => {
            newEtag(etag, response.json)
            response.json
          }
          case _ => response.json
        }
      } else response.json
    }
  }

  def cachedResponseOrElse(url: String)(implicit response: Response): JsValue = {
    cachedResponseFrom(
      url,
      (etag, res) => cacheWithEtag(url, etag, res),
      (lastModified, res) => cacheWithLastModified(url, lastModified, res)
    )
  }

  def cacheWithLastModified(url: String, lastModified: Date, response: JsValue) {
    Cache.set(KEY_LASTMODIFIED + url, lastModifiedFormat.format(lastModified), Config.Cache.expiration)
    Cache.set(KEY_RESPONSE + url, response, Config.Cache.expiration)
  }

  def cacheWithEtag(url: String, etag: String, response: JsValue) {
    Cache.set(KEY_ETAG + url, etag, Config.Cache.expiration)
    Cache.set(KEY_RESPONSE + url, response, Config.Cache.expiration)
  }

  def lastModifiedFor(url: String): Seq[(String, String)] = {
    Cache.getAs[String](KEY_LASTMODIFIED + url) map { lastModified =>
      Seq("If-Modified-Since" -> lastModified)
    } getOrElse Nil
  }

  def etagFor(url: String): Seq[(String, String)] = {
    Cache.getAs[String](KEY_ETAG + url) map { etag =>
      Seq("If-None-Match" -> etag)
    } getOrElse Nil
  }
}
