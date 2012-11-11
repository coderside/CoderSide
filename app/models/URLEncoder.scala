package models

import java.net.{ URLEncoder => JURLEncoder }

trait URLEncoder {
  def encode(value: String) = JURLEncoder.encode(value, "UTF-8")
}
