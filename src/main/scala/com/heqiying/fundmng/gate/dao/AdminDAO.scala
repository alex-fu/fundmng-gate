package com.heqiying.fundmng.gate.dao

import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.model.{ Admin, DBSchema }
import com.heqiying.fundmng.gate.database.MainDBProfile._
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._

object AdminDAO extends LazyLogging {
  private val adminsQ = DBSchema.admins

  def getAll = {
    db.run(adminsQ.result)
  }

  def insert(admin: Admin) = {
    val q = adminsQ += admin
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  def update(adminId: Int, password: String, adminName: String, email: String) = {
    val q = adminsQ.filter(_.id === adminId).map(x => (x.password, x.adminName, x.email))
      .update(password, adminName, email)
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  def getOne(id: Int) = {
    val q = adminsQ.filter(_.id === id).result
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q.headOption)
  }
}
