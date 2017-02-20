package com.heqiying.fundmng.gate.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import slick.profile.SqlProfile.ColumnOption.Nullable
import spray.json.DefaultJsonProtocol

case class Uploads(uuid: String, name: String, contentType: Option[String], path: String, md5: String, size: Long)

class UploadsTable(tag: Tag) extends Table[Uploads](tag, "uploads") {
  def uuid = column[String]("uuid", O.Length(40, varying = false), O.PrimaryKey)
  def name = column[String]("name", O.Length(127))
  def contentType = column[String]("content_type", O.Length(63), Nullable)
  def path = column[String]("path", O.Length(255))
  def md5 = column[String]("md5", O.Length(40, varying = false))
  def size = column[Long]("size")

  def idxName = index("idx_name", name, unique = false)

  def * = (uuid, name, contentType.?, path, md5, size) <> (Uploads.tupled, Uploads.unapply)
}

object UploadsJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val uploadsJsFormat = jsonFormat6(Uploads.apply)
}
