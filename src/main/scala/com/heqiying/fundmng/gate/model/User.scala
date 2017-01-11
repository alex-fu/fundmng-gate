package com.heqiying.fundmng.gate.model

/**
 * This file is only for POC, not needed in fundmng-gate project
 */

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

case class User(username: String, password: String)

object UserJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val userJsonFormat: RootJsonFormat[User] = jsonFormat2(User.apply)
}
