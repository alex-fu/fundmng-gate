package com.heqiying.fundmng.gate.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import slick.profile.SqlProfile.ColumnOption.Nullable
import spray.json._

case class Admin(loginName: String, password: String, adminName: String,
  email: String, wxid: Option[String], createAt: Long, updateAt: Long)

case class Accesser(loginName: String, name: Option[String], email: Option[String], wxid: Option[String], groupType: String)

case class Group(groupName: String, groupType: String)

object Groups {
  val GroupTypeAdmin = "AdminGroup"
  val GroupTypeInvestor = "InvestorGroup"

  val GroupNameInputer = "Inputer"
  val GroupNameReviewer = "Reviewer"
  val GroupNameSystemAdmin = "SystemAdmin"
}

case class GroupAdminMapping(mappingId: Option[Int], groupName: String, adminName: String)

case class AuthorityExpression(httpMethods: Seq[String], pathExpression: String)

case class Authority(authorityName: String, authorityLabel: String, expressions: Seq[AuthorityExpression])

case class AuthorityGroupMapping(mappingId: Option[Int], authorityName: String, groupName: String)

class Admins(tag: Tag) extends Table[Admin](tag, "admins") {
  def loginName = column[String]("login_name", O.Length(127, varying = false), O.PrimaryKey)

  def password = column[String]("password", O.Length(255, varying = true))

  def adminName = column[String]("admin_name", O.Length(255, varying = true))

  def email = column[String]("email", O.Length(127, varying = true))

  def wxid = column[String]("wxid", Nullable)

  def createdAt = column[Long]("created_at")

  def updatedAt = column[Long]("updated_at")

  def * = (loginName, password, adminName, email, wxid.?, createdAt, updatedAt) <> (Admin.tupled, Admin.unapply)
}

class Groups(tag: Tag) extends Table[Group](tag, "groups") {
  def groupName = column[String]("group_name", O.Length(127, varying = false), O.PrimaryKey)

  def groupType = column[String]("group_type", O.Length(127, varying = true))

  def * = (groupName, groupType) <> (Group.tupled, Group.unapply)
}

class GroupAdminMappings(tag: Tag) extends Table[GroupAdminMapping](tag, "group_admin_mappings") {
  def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

  def groupName = column[String]("group_name", O.Length(127, varying = false))

  def loginName = column[String]("login_name", O.Length(127, varying = false))

  def foreignKeyGroup = foreignKey("GA_GRP_FK", groupName, DBSchema.groups)(_.groupName, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)

  def foreignKeyAdmin = foreignKey("GA_ADM_FK", loginName, DBSchema.admins)(_.loginName, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)

  def idxGroupAdmin = index("idx_groupadmin", (groupName, loginName), unique = true)

  def * = (id.?, groupName, loginName) <> (GroupAdminMapping.tupled, GroupAdminMapping.unapply)
}

class Authorities(tag: Tag) extends Table[Authority](tag, "authorities") {
  def authorityName = column[String]("authority_name", O.Length(127, varying = false), O.PrimaryKey)

  def authorityLabel = column[String]("authority_label", O.Length(255, varying = true))

  /**
   * expressions: use json string
   */
  def expressions = column[String]("expressions", Nullable)

  import Authorities._
  def * = (authorityName, authorityLabel, expressions) <> (toAuthority, fromAuthority)
}

object Authorities {
  import AuthorityRegressionExpressionJsonSupport._
  def toAuthority(t: Tuple3[String, String, String]): Authority = {
    val (name, label, re) = t
    val reSeq = re.parseJson.convertTo[Seq[AuthorityExpression]]
    Authority(name, label, reSeq)
  }

  def fromAuthority(authority: Authority): Option[Tuple3[String, String, String]] = {
    require(authority.authorityName.nonEmpty)
    require(authority.authorityLabel.nonEmpty)
    Some((authority.authorityName, authority.authorityLabel, authority.expressions.toJson.compactPrint))
  }
}

class AuthorityGroupMappings(tag: Tag) extends Table[AuthorityGroupMapping](tag, "authority_group_mappings") {
  def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

  def authorityName = column[String]("authority_name", O.Length(127, varying = false))

  def groupName = column[String]("group_name", O.Length(127, varying = false))

  def foreignKeyGroupId = foreignKey("AG_GRP_FK", groupName, DBSchema.groups)(_.groupName, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)

  def idxAuthorityGroup = index("idx_authoritygroup", (authorityName, groupName), unique = true)

  def * = (id.?, authorityName, groupName) <> (AuthorityGroupMapping.tupled, AuthorityGroupMapping.unapply)
}

object AdminJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val adminJsonFormat: RootJsonFormat[Admin] = jsonFormat7(Admin.apply)
}

object AccesserJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val accesserJsonFormat = jsonFormat5(Accesser.apply)
}

object GroupJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val groupJsonFormat: RootJsonFormat[Group] = jsonFormat2(Group.apply)
}

object AuthorityRegressionExpressionJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val regressionExpressionJsonFormat: RootJsonFormat[AuthorityExpression] = jsonFormat2(AuthorityExpression.apply)
}

object AuthorityJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  import AuthorityRegressionExpressionJsonSupport.regressionExpressionJsonFormat
  implicit val authorityJsonFormat: RootJsonFormat[Authority] = jsonFormat3(Authority.apply)
}

