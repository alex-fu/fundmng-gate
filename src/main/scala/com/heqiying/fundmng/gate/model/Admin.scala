package com.heqiying.fundmng.gate.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import slick.profile.SqlProfile.ColumnOption.Nullable
import spray.json._

case class Admin(adminId: Option[Int], loginName: String, password: String, adminName: String,
  email: String, wxid: Option[String], createAt: Long, updateAt: Option[Long])

case class Accesser(loginName: String, name: Option[String], email: Option[String], wxid: Option[String])

case class Group(groupId: Option[Int], groupName: String, groupType: String)

object Groups {
  val GroupTypeAdmin = "AdminGroup"
  val GroupTypeInvestor = "InvestorGroup"
}

case class GroupAdminMapping(mappingId: Option[Int], groupId: Int, adminId: Int)

case class AuthorityRegressionExpression(httpMethods: Seq[String], pathExpression: String)

case class Authority(authorityName: String, authorityLabel: String, regressionExpressions: Seq[AuthorityRegressionExpression])

case class AuthorityGroupMapping(mappingId: Option[Int], authorityName: String, groupId: Int)

class Admins(tag: Tag) extends Table[Admin](tag, "admins") {
  def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

  def loginName = column[String]("loginName", O.Length(255, varying = true))

  def password = column[String]("password", O.Length(255, varying = true))

  def adminName = column[String]("adminName", O.Length(255, varying = true))

  def email = column[String]("email", O.Length(127, varying = true))

  def wxid = column[String]("wxid", Nullable)

  def createdAt = column[Long]("created_at")

  def updatedAt = column[Long]("updated_at", Nullable)

  def idxLoginName = index("idx_loginName", loginName, unique = true)

  def * = (id.?, loginName, password, adminName, email, wxid.?, createdAt, updatedAt.?) <> (Admin.tupled, Admin.unapply)
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

  def idxGroupAdmin = index("idx_groupadmin", (groupId, adminId), unique = true)

  def * = (id.?, groupId, adminId) <> (GroupAdminMapping.tupled, GroupAdminMapping.unapply)
}

class Authorities(tag: Tag) extends Table[Authority](tag, "authorities") {
  def authorityName = column[String]("authority_name", O.Length(255, varying = true), O.PrimaryKey)

  def authorityLabel = column[String]("authority_label", O.Length(255, varying = true))

  /**
   * regressionExpressions: use json string
   */
  def regressionExpressions = column[String]("regression_expressions", Nullable)

  import Authorities._
  def * = (authorityName, authorityLabel, regressionExpressions) <> (toAuthority, fromAuthority)
}

object Authorities {
  import AuthorityRegressionExpressionJsonSupport._
  def toAuthority(t: Tuple3[String, String, String]): Authority = {
    val (name, label, re) = t
    val reSeq = re.parseJson.convertTo[Seq[AuthorityRegressionExpression]]
    Authority(name, label, reSeq)
  }

  def fromAuthority(authority: Authority): Option[Tuple3[String, String, String]] = {
    require(authority.authorityName.nonEmpty)
    require(authority.authorityLabel.nonEmpty)
    Some((authority.authorityName, authority.authorityLabel, authority.regressionExpressions.toJson.compactPrint))
  }
}

class AuthorityGroupMappings(tag: Tag) extends Table[AuthorityGroupMapping](tag, "authority_group_mappings") {
  def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

  def authorityName = column[String]("authority_name", O.Length(255, varying = true))

  def groupId = column[Int]("group_id")

  def foreignKeyGroupId = foreignKey("AG_GRPID_FK", groupId, DBSchema.groups)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

  def idxAuthorityGroup = index("idx_authoritygroup", (authorityName, groupId), unique = true)

  def * = (id.?, authorityName, groupId) <> (AuthorityGroupMapping.tupled, AuthorityGroupMapping.unapply)
}

object AdminJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val adminJsonFormat: RootJsonFormat[Admin] = jsonFormat8(Admin.apply)
}

object AccesserJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val accesserJsonFormat = jsonFormat4(Accesser.apply)
}

object GroupJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val groupJsonFormat: RootJsonFormat[Group] = jsonFormat3(Group.apply)
}

object AuthorityRegressionExpressionJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val regressionExpressionJsonFormat: RootJsonFormat[AuthorityRegressionExpression] = jsonFormat2(AuthorityRegressionExpression.apply)
}

object AuthorityJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  import AuthorityRegressionExpressionJsonSupport.regressionExpressionJsonFormat
  implicit val authorityJsonFormat: RootJsonFormat[Authority] = jsonFormat3(Authority.apply)
}

