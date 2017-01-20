package com.heqiying.fundmng.gate.model.activiti

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

case class User(id: String, firstName: String, lastName: String, email: String, password: Option[String], url: Option[String])

case class Group(id: String, name: String, `type`: String, url: Option[String])

case class Member(userId: String)

object UserJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val userJsonFormat = jsonFormat6(User.apply)
}

object GroupJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val groupJsonFormat = jsonFormat4(Group.apply)
}

object MemberJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val memberJsonFormat = jsonFormat1(Member.apply)
}