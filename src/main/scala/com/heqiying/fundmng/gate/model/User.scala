package com.heqiying.fundmng.gate.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

case class User(username: String, password: String)

class Users(tag: Tag) extends Table[User](tag, "users") {
  def username = column[String]("username", O.Length(255), O.PrimaryKey)
  def password = column[String]("password", O.Length(255))

  def idxUserName = index("idx_username", username, unique = true)

  def * = (username, password) <> (User.tupled, User.unapply)
}

object UserJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val userJsonFormat: RootJsonFormat[User] = jsonFormat2(User.apply)
}
