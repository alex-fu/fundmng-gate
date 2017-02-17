package com.heqiying.fundmng.gate.service

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.util.FastFuture._
import akka.stream.ActorMaterializer
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.dao.{ ActiIdentityRPC, AdminDAO, GroupDAO }
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import com.heqiying.fundmng.gate.model.{ Accesser, Admin, UpdateAdminRequest }
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

  def updateAdmin(loginName: String, req: UpdateAdminRequest, accesser: Option[Accesser]) = {
    def merge(orig: Admin, req: UpdateAdminRequest) = {
      Admin(
        orig.loginName,
        req.password.getOrElse(orig.password),
        req.adminName.getOrElse(orig.adminName),
        req.email.getOrElse(orig.email),
        req.wxid.orElse(orig.wxid),
        orig.createdAt,
        Some(System.currentTimeMillis())
      )
    }
    logger.info(s"update admin $loginName as $req")
    val actiRPC = ActiIdentityRPC.create(accesser)
    val r = for {
      orig <- getAdminByName(loginName) if orig.nonEmpty
      merged = merge(orig.get, req)
      r1 <- actiRPC.updateUser(loginName, merged.adminName, merged.email)
      if r1.status == StatusCodes.OK
      _ <- AdminDAO.update(loginName, merged)
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

  def getAdminGroupNames(loginName: String) = {
    GroupDAO.getGroupsForAdmin(loginName)
  }

  def getAdminGroups(loginName: String) = {
    val groupNames = GroupDAO.getGroupsForAdmin(loginName)
    val groups = groupNames.flatMap { xs =>
      Future.sequence(xs.map { groupName =>
        GroupDAO.getOne(groupName)
      })
    }.map(_.flatten)
    groups
  }
}
