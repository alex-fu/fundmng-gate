package com.heqiying.fundmng.gate.service

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.dao.{ ActiIdentityRPC, AdminDAO, AuthorityDAO, GroupDAO }
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import com.heqiying.fundmng.gate.model.{ Accesser, Group }
import com.heqiying.fundmng.gate.utils.QueryParam

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait GroupService extends LazyLogging {
  implicit val system: ActorSystem
  implicit val mat: ActorMaterializer

  def addGroup(group: Group, accesser: Option[Accesser]) = {
    logger.info(s"add new group: $group")
    val actiRPC = ActiIdentityRPC.create(accesser)
    val f = for {
      r1 <- actiRPC.createGroup(group.groupName, group.groupName, group.groupType)
      if Seq(StatusCodes.Created, StatusCodes.Conflict) contains r1.status
      _ <- GroupDAO.insert(group)
    } yield ()
    f
  }

  def deleteGroup(groupName: String, accesser: Option[Accesser]) = {
    logger.info(s"delete group: $groupName")
    val actiRPC = ActiIdentityRPC.create(accesser)
    val f = for {
      r1 <- actiRPC.deleteGroup(groupName)
      if Seq(StatusCodes.NoContent, StatusCodes.NotFound) contains r1.status
      _ <- GroupDAO.delete(groupName)
    } yield ()
    f
  }

  def getGroups(qp: QueryParam) = {
    GroupDAO.get(qp)
  }

  def getGroupByName(groupName: String) = {
    GroupDAO.getOne(groupName)
  }

  def updateAdminsInGroup(groupName: String, adminNames: Seq[String], accesser: Option[Accesser]) = {
    logger.info(s"update admins in group $groupName with $adminNames")
    val actiRPC = ActiIdentityRPC.create(accesser)
    val f = for {
      existedAdminNames <- GroupDAO.getAdminsInGroup(groupName)
      s0 = existedAdminNames.toSet
      s1 = adminNames.toSet
      toDeletes = s0 -- s1
      toAdds = s1 -- s0
      r1 <- Future.sequence(toDeletes.map(ys => actiRPC.deleteMemberFromGroup(groupName, ys)))
      if r1.forall(r => Seq(StatusCodes.NoContent, StatusCodes.NotFound).contains(r.status))
      r2 <- Future.sequence(toAdds.map(ys => actiRPC.addMemberToGroup(groupName, ys)))
      if r2.forall(r => Seq(StatusCodes.Created, StatusCodes.Conflict).contains(r.status))
      _ <- GroupDAO.postAdminsInGroup(groupName, adminNames)
    } yield ()
    f
  }

  def getAdminsInGroup(groupName: String) = {
    val adminNames = GroupDAO.getAdminsInGroup(groupName)
    val admins = adminNames.flatMap { xs =>
      Future.sequence(xs.map { adminName =>
        AdminDAO.getOne(adminName)
      })
    }.map(_.flatten)
    admins
  }

  def updateAuthoritiesOnGroup(groupName: String, authorityNames: Seq[String]) = {
    logger.info(s"update authorities on group $groupName with $authorityNames")
    AuthorityDAO.postAuthoritiesInGroup(groupName, authorityNames)
  }

  def getAuthoritiesOnGroup(groupName: String) = {
    val authorityNames: Future[Seq[String]] = AuthorityDAO.getAuthoritiesInGroup(groupName)
    val authorities = authorityNames.flatMap { xs =>
      Future.sequence(xs.map { authName =>
        AuthorityDAO.getOne(authName)
      })
    }.map(_.flatten)
    authorities
  }

  def updateAuthoritiesOnInvestorGroup(authorityNames: Seq[String]) = {
    logger.info(s"update authorities on investor group with $authorityNames")
    val f = for {
      investorGroup <- GroupDAO.getOrCreateInvestorGroup()
      if investorGroup.nonEmpty
      r <- AuthorityDAO.postAuthoritiesInGroup(investorGroup.get.groupName, authorityNames)
    } yield r
    f
  }

  def getAuthoritiesOnInvestorGroup() = {
    val authorityNames: Future[Seq[String]] =
      for {
        investorGroup <- GroupDAO.getOrCreateInvestorGroup()
        if investorGroup.nonEmpty
        authorityNames <- AuthorityDAO.getAuthoritiesInGroup(investorGroup.get.groupName)
      } yield authorityNames
    val authorities = authorityNames.flatMap { xs =>
      Future.sequence(xs.map { authName =>
        AuthorityDAO.getOne(authName)
      })
    }.map(_.flatten)
    authorities
  }
}
