package com.heqiying.fundmng.gate.dao

import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import com.heqiying.fundmng.gate.database.MainDBProfile._
import com.heqiying.fundmng.gate.model._

object AuthorityDAO extends LazyLogging {
  private val authoritiesQ = DBSchema.authorities
  private val authorityGroupMappingsQ = DBSchema.authorityGroupMapping

  def getAll = {
    db.run(authoritiesQ.result)
  }

  def getOne(authorityName: String) = {
    val q = authoritiesQ.filter(_.authorityName === authorityName).result
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q.headOption)
  }

  def upsert(authorities: Iterable[Authority]) = {
    val q1 = authoritiesQ.delete
    val q2 = authoritiesQ ++= authorities
    val q = DBIO.seq(q1, q2).transactionally
    sqlDebug(q1.statements.mkString(";\n"))
    sqlDebug(q2.statements.mkString(";\n"))
    db.run(q)
  }

  def getAuthoritiesInGroup(groupId: Int) = {
    val q = authorityGroupMappingsQ.filter(_.groupId === groupId).map(_.authorityName).result
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  def postAuthoritiesInGroup(groupId: Int, authorityNames: Iterable[String]) = {
    val q1 = authorityGroupMappingsQ.filter(_.groupId === groupId).delete
    val mappings = authorityNames.map(authorityName => AuthorityGroupMapping(None, authorityName, groupId))
    val q2 = authorityGroupMappingsQ ++= mappings
    val q = DBIO.seq(q1, q2).transactionally
    sqlDebug(q1.statements.mkString(";\n"))
    sqlDebug(q2.statements.mkString(";\n"))
    db.run(q)
  }
}
