package com.heqiying.fundmng.gate.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

case class User(username: String, password: String)

object UserJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val userFormat = jsonFormat2(User.apply)
}
