package com.heqiying.fundmng.gate.service

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.util.FastFuture._
import akka.stream.ActorMaterializer
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.dao.{ActiIdentityRPC, AdminDAO, GroupDAO}
import com.heqiying.fundmng.gate.model.{Accesser, Admin}
import com.heqiying.fundmng.gate.utils.QueryParam

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AdminService extends LazyLogging {
  implicit val system: ActorSystem
  implicit val mat: ActorMaterializer

  def addAdmin(admin: Admin, accesser: Option[Accesser]) = {
    val adminO = AdminDAO.getOne(admin.loginName)
    val r = adminO.fast.flatMap {
      case None =>
        logger.info(s"add new admin: ${admin.toString}")
        val actiRPC = ActiIdentityRPC.create(accesser)
        for {
          r1 <- actiRPC.createUser(admin.loginName, admin.adminName, admin.email)
          if Seq(StatusCodes.Created, StatusCodes.Conflict).contains(r1.status)
          _ <- AdminDAO.insert(admin)
        } yield Right(admin.copy(password = ""))
      case _ => Future(Left("已存在相同管理员"))
    }
    r
  }

  def updateAdmin(loginName: String, password: String, adminName: String, email: String, wxid: Option[String], accesser: Option[Accesser]) = {
    logger.info(s"update admin $loginName($password, $adminName, $email, $wxid)")
    val actiRPC = ActiIdentityRPC.create(accesser)
    val r = for {
      r1 <- actiRPC.updateUser(loginName, adminName, email)
      if r1.status == StatusCodes.OK
      _ <- AdminDAO.update(loginName, password, adminName, email, wxid)
    } yield ()
    r
  }

  def deleteAdmin(loginName: String, accesser: Option[Accesser]) = {
    logger.info(s"delete admin $loginName")
    val actiRPC = ActiIdentityRPC.create(accesser)
    val r = for {
      r1 <- actiRPC.deleteUser(loginName)
      if Seq(StatusCodes.NoContent, StatusCodes.NotFound) contains r1.status
      _ <- AdminDAO.delete(loginName)
    } yield ()
    r
  }

  def getAdmins(qp: QueryParam) = {
    AdminDAO.get(qp)
  }

  def getAdminByName(loginName: String) = {
    AdminDAO.getOne(loginName)
  }

  def getAdminGroups(loginName: String) = {
    val groupIds = GroupDAO.getGroupsForAdmin(loginName)
    val groups = groupIds.flatMap { xs =>
      Future.sequence(xs.map { groupid =>
        GroupDAO.getOne(groupid)
      })
    }.map(_.flatten)
    groups
  }
}
