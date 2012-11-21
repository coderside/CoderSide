package models

import play.api.libs.ws.Response

trait Debug {
  def debug(response: Response): Response = {
    println("--------------------")
    println(response.json)
    println("--------------------")
    response
  }
}
