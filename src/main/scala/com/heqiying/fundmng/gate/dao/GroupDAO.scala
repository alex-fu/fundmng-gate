package com.heqiying.fundmng.gate.dao

import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.model.{ AdminID, DBSchema, Group, GroupAdminMapping }
import com.heqiying.fundmng.gate.database.MainDBProfile._
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._

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

  def postAdminsInGroup(groupId: Int, adminIds: Iterable[AdminID]) = {
    val mappings = adminIds.map(adminId => GroupAdminMapping(None, groupId, adminId.adminId))
    val q = groupAdminMappingsQ ++= mappings
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  def getGroupsForAdmin(adminId: Int) = {
    val q = groupAdminMappingsQ.filter(_.adminId === adminId).map(_.groupId).result
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }
}
