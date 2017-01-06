package com.heqiying.fundmng.gate.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import slick.profile.SqlProfile.ColumnOption.Nullable
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

case class Admin(adminId: Option[Int], loginName: String, password: String, adminName: String,
  email: String, createAt: Long, updateAt: Option[Long])

/**
 * groupType: "AdminGroup" or "InvestorGroup"
 */
case class Group(groupId: Option[Int], groupName: String, groupType: String)

case class GroupAdminMapping(mappingId: Option[Int], groupId: Int, adminId: Int)

case class Authority(authorityId: Option[Int], authorityName: String)

case class AuthorityGroupMapping(mappingId: Option[Int], authorityId: Int, groupId: Int)

class Admins(tag: Tag) extends Table[Admin](tag, "admins") {
  def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

  def loginName = column[String]("loginName", O.Length(255, varying = true))

  def password = column[String]("password", O.Length(255, varying = true))

  def adminName = column[String]("adminName", O.Length(255, varying = true))

  def email = column[String]("email", O.Length(127, varying = true))

  def createdAt = column[Long]("created_at")

  def updatedAt = column[Long]("updated_at", Nullable)

  def idxLoginName = index("idx_loginName", loginName, unique = true)

  def * = (id.?, loginName, password, adminName, email, createdAt, updatedAt.?) <> (Admin.tupled, Admin.unapply)
}

class Groups(tag: Tag) extends Table[Group](tag, "groups") {
  def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

  def groupName = column[String]("group_name", O.Length(255, varying = true))

  def groupType = column[String]("group_type", O.Length(127, varying = true))

  def idxGroupName = index("idx_groupname", groupName, unique = true)

  def * = (id.?, groupName, groupType) <> (Group.tupled, Group.unapply)
}

class GroupAdminMappings(tag: Tag) extends Table[GroupAdminMapping](tag, "group_admin_mappings") {
  def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

  def groupId = column[Int]("group_id")

  def adminId = column[Int]("admin_id")

  def foreignKeyGroupId = foreignKey("GA_GRPID_FK", groupId, DBSchema.groups)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

  def foreignKeyAdminId = foreignKey("GA_ADMID_FK", adminId, DBSchema.admins)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

  def * = (id.?, groupId, adminId) <> (GroupAdminMapping.tupled, GroupAdminMapping.unapply)
}

class Authorities(tag: Tag) extends Table[Authority](tag, "authorities") {
  def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

  def authorityName = column[String]("authority_name", O.Length(255, varying = true))

  def * = (id.?, authorityName) <> (Authority.tupled, Authority.unapply)
}

class AuthorityGroupMappings(tag: Tag) extends Table[AuthorityGroupMapping](tag, "authority_group_mappings") {
  def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

  def authorityId = column[Int]("authority_id")

  def groupId = column[Int]("group_id")

  def foreignKeyAuthorityId = foreignKey("AG_AUTHID_FK", authorityId, DBSchema.authorities)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

  def foreignKeyGroupId = foreignKey("AG_GRPID_FK", groupId, DBSchema.groups)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

  def * = (id.?, authorityId, groupId) <> (AuthorityGroupMapping.tupled, AuthorityGroupMapping.unapply)
}

object AdminJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val adminJsonFormat: RootJsonFormat[Admin] = jsonFormat7(Admin.apply)
}

object GroupJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val groupJsonFormat: RootJsonFormat[Group] = jsonFormat3(Group.apply)
}

object AuthorityJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val authorityJsonFormat: RootJsonFormat[Authority] = jsonFormat2(Authority.apply)
}

