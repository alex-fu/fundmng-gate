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

  def update(groupId: Int, groupName: String, groupType: String) = {
    val q = groupsQ.filter(_.id === groupId).map(x => (x.groupName, x.groupType))
      .update(groupName, groupType)
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  def getOne(id: Int) = {
    val q = groupsQ.filter(_.id === id).result
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q.headOption)
  }

  def delete(id: Int) = {
    val q = groupsQ.filter(_.id === id).delete
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  def getAdminsInGroup(groupId: Int) = {
    val q = groupAdminMappingsQ.filter(_.groupId === groupId).map(_.adminId).result
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  def postAdminsInGroup(groupId: Int, adminIds: Iterable[Int]) = {
    val q1 = groupAdminMappingsQ.filter(_.groupId === groupId).delete
    val mappings = adminIds.map(adminId => GroupAdminMapping(None, groupId, adminId))
    val q2 = groupAdminMappingsQ ++= mappings
    val q = DBIO.seq(q1, q2).transactionally
    sqlDebug(q1.statements.mkString(";\n"))
    sqlDebug(q2.statements.mkString(";\n"))
    db.run(q)
  }

  def getGroupsForAdmin(adminId: Int) = {
    val q = groupAdminMappingsQ.filter(_.adminId === adminId).map(_.groupId).result
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  private def getInvestorGroup() = {
    val q1 = groupsQ.filter(_.groupType === Groups.GroupTypeInvestor).result.headOption
    sqlDebug(q1.statements.mkString(";\n"))
    db.run(q1)
  }

  private def createInvestorGroup() = {
    insert(Group(None, "GlobalInvestorGroup", Groups.GroupTypeInvestor))
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
