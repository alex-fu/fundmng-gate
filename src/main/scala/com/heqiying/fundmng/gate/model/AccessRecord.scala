package com.heqiying.fundmng.gate.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import slick.lifted.Tag
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import slick.profile.SqlProfile.ColumnOption.Nullable
import spray.json.DefaultJsonProtocol

case class AccessRecord(id: Option[Int], time: Long, uri: String, method: String,
  who: Option[String], from: Option[String], jwt: Option[String],
  responseStatus: Int)

class AccessRecords(tag: Tag) extends Table[AccessRecord](tag, "access_records") {
  def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

  def time = column[Long]("time")

  def uri = column[String]("uri", O.Length(255))

  def method = column[String]("method", O.Length(15, varying = false))

  def who = column[String]("who", O.Length(127), Nullable)

  def from = column[String]("from", O.Length(31), Nullable)

  def jwt = column[String]("jwt", Nullable)

  def responseStatus = column[Int]("response_status")

  def * = (id.?, time, uri, method, who.?, from.?, jwt.?, responseStatus) <> (AccessRecord.tupled, AccessRecord.unapply)
}

object AccessRecordJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val accessRecordJsonFormat = jsonFormat8(AccessRecord.apply)
}

