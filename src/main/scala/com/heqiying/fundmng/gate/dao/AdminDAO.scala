package com.heqiying.fundmng.gate.dao

import akka.http.scaladsl.model.StatusCodes
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.model.{ Admin, DBSchema }
import com.heqiying.fundmng.gate.model.activiti
import com.heqiying.fundmng.gate.database.MainDBProfile._
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import slick.dbio.DBIOAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object AdminDAO extends LazyLogging {
  private val adminsQ = DBSchema.admins

  def getAll = {
    db.run(adminsQ.result)
  }

  def insert(admin: Admin, actiRPCO: Option[ActiIdentityRPC]): Future[Int] = {
    val q1 = adminsQ += admin
    sqlDebug(q1.statements.mkString(";\n"))
    val q = actiRPCO match {
      case Some(actiRPC) =>
        for {
          r <- q1
          r1 <- DBIO.from(actiRPC.createUser(admin.loginName, admin.adminName, admin.email))
          if Seq(StatusCodes.Created, StatusCodes.Conflict).contains(r1.status)
        } yield r
      case None => q1
    }
    db.run(q.transactionally)
  }

  def update(adminId: Int, password: String, adminName: String, email: String, wxid: Option[String], actiRPCO: Option[ActiIdentityRPC]) = {
    val q1 = adminsQ.filter(_.id === adminId).result.head
    val q2 = adminsQ.filter(_.id === adminId).map(x => (x.password, x.adminName, x.email, x.wxid, x.updatedAt))
      .update(password, adminName, email, wxid.getOrElse(""), System.currentTimeMillis())
    sqlDebug(q2.statements.mkString(";\n"))
    val q = actiRPCO match {
      case Some(actiRPC) =>
        for {
          admin <- q1
          r <- q2
          r1 <- DBIO.from(actiRPC.updateUser(admin.loginName, adminName, email))
          if r1.status == StatusCodes.OK
        } yield r
      case None => q2
    }
    db.run(q.transactionally)
  }

  def getOne(id: Int) = {
    val q = adminsQ.filter(_.id === id).result
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q.headOption)
  }

  def getOneByLoginName(loginName: String) = {
    val q = adminsQ.filter(_.loginName === loginName).result
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q.headOption)
  }

  def delete(id: Int, actiRPCO: Option[ActiIdentityRPC]) = {
    val q1 = adminsQ.filter(_.id === id).result.head
    val q2 = adminsQ.filter(_.id === id).delete
    sqlDebug(q2.statements.mkString(";\n"))
    val q = actiRPCO match {
      case Some(actiRPC) =>
        for {
          admin <- q1
          r <- q2
          r1 <- DBIO.from(actiRPC.deleteUser(admin.loginName))
          if Seq(StatusCodes.NoContent, StatusCodes.NotFound) contains r1.status
        } yield r
      case None => q2
    }
    db.run(q.transactionally)
  }
}
