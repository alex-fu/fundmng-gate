package com.heqiying.fundmng.gate.dao

import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.database.MainDBProfile._
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import com.heqiying.fundmng.gate.model.{Admin, Admins, DBSchema}

import scala.concurrent.Future

object AdminDAO extends ComplexQuery[Admins] with LazyLogging {
  private val adminsQ = DBSchema.admins
  override val tableQ = adminsQ

  def getAll = {
    db.run(adminsQ.result)
  }

  def insert(admin: Admin): Future[Int] = {
    val q = adminsQ += admin
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  def update(loginName: String, password: String, adminName: String, email: String, wxid: Option[String]) = {
    val q = adminsQ.filter(_.loginName === loginName).map(x => (x.password, x.adminName, x.email, x.wxid, x.updatedAt))
      .update(password, adminName, email, wxid.getOrElse(""), System.currentTimeMillis())
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  def getOne(loginName: String) = {
    val q = adminsQ.filter(_.loginName === loginName).result
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q.headOption)
  }

  def delete(loginName: String) = {
    val q = adminsQ.filter(_.loginName === loginName).delete
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }
}
