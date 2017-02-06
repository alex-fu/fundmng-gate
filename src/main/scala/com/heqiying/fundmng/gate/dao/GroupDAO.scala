package com.heqiying.fundmng.gate.dao

import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.database.MainDBProfile._
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import com.heqiying.fundmng.gate.model.{ DBSchema, Group, GroupAdminMapping, Groups }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object GroupDAO extends LazyLogging {
  private val groupsQ = DBSchema.groups
  private val groupAdminMappingsQ = DBSchema.groupAdminMappings

  def getAll = {
    db.run(groupsQ.result)
  }

  def insert(group: Group) = {
    val q = groupsQ += group
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  def update(groupName: String, groupType: String) = {
    val q = groupsQ.filter(_.groupName === groupName).map(x => (x.groupName, x.groupType))
      .update(groupName, groupType)
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  def getOne(groupName: String) = {
    val q = groupsQ.filter(_.groupName === groupName).result
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q.headOption)
  }

  def delete(groupName: String) = {
    val q = groupsQ.filter(_.groupName === groupName).delete
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  def getAdminsInGroup(groupName: String) = {
    val q = groupAdminMappingsQ.filter(_.groupName === groupName).map(_.loginName).result
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  def postAdminsInGroup(groupName: String, adminNames: Iterable[String]) = {
    val q1 = groupAdminMappingsQ.filter(_.groupName === groupName).delete
    val mappings = adminNames.map(adminName => GroupAdminMapping(None, groupName, adminName))
    val q2 = groupAdminMappingsQ ++= mappings
    sqlDebug(q1.statements.mkString(";\n"))
    sqlDebug(q2.statements.mkString(";\n"))

    val q = DBIO.seq(q1, q2)
    db.run(q.transactionally)
  }

  def getGroupsForAdmin(adminName: String) = {
    val q = groupAdminMappingsQ.filter(_.loginName === adminName).map(_.groupName).result
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  // Investor Group Jobs
  def getInvestorGroup() = {
    val q1 = groupsQ.filter(_.groupType === Groups.GroupTypeInvestor).result.headOption
    sqlDebug(q1.statements.mkString(";\n"))
    db.run(q1)
  }

  private def createInvestorGroup() = {
    insert(Group("GlobalInvestorGroup", Groups.GroupTypeInvestor))
  }

  def getOrCreateInvestorGroup(): Future[Option[Group]] = {
    val r = for {
      groupO <- getInvestorGroup()
      if groupO.nonEmpty
    } yield groupO

    r.recoverWith {
      case _ =>
        for {
          _ <- createInvestorGroup()
          groupO <- getInvestorGroup()
        } yield groupO
    }
  }
}
