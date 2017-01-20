package com.heqiying.fundmng.gate.dao

import akka.http.scaladsl.model.StatusCodes
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

  def insert(group: Group, actiRPCO: Option[ActiIdentityRPC]) = {
    val q1 = groupsQ += group
    sqlDebug(q1.statements.mkString(";\n"))
    val q = actiRPCO match {
      case Some(actiRPC) =>
        for {
          r <- q1
          r1 <- DBIO.from(actiRPC.createGroup(group.groupName, group.groupName, group.groupType))
          if Seq(StatusCodes.Created, StatusCodes.Conflict) contains r1.status
        } yield r
      case None => q1
    }
    db.run(q.transactionally)
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

  def delete(id: Int, actiRPCO: Option[ActiIdentityRPC]) = {
    val q1 = groupsQ.filter(_.id === id).result.head
    val q2 = groupsQ.filter(_.id === id).delete
    sqlDebug(q2.statements.mkString(";\n"))
    val q = actiRPCO match {
      case Some(actiRPC) =>
        for {
          group <- q1
          r <- q2
          r1 <- DBIO.from(actiRPC.deleteGroup(group.groupName))
          if Seq(StatusCodes.NoContent, StatusCodes.NotFound) contains r1.status
        } yield r
      case None => q2
    }
    db.run(q.transactionally)
  }

  def getAdminsInGroup(groupId: Int) = {
    val q = groupAdminMappingsQ.filter(_.groupId === groupId).map(_.adminId).result
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  def postAdminsInGroup(groupId: Int, adminIds: Iterable[Int], actiRPCO: Option[ActiIdentityRPC]) = {
    val q0 = groupAdminMappingsQ.filter(_.groupId === groupId).result
    val q1 = groupAdminMappingsQ.filter(_.groupId === groupId).delete
    val mappings = adminIds.map(adminId => GroupAdminMapping(None, groupId, adminId))
    val q2 = groupAdminMappingsQ ++= mappings
    sqlDebug(q0.statements.mkString(";\n"))
    sqlDebug(q1.statements.mkString(";\n"))
    sqlDebug(q2.statements.mkString(";\n"))

    def zipGroupAdmin(mappings: Iterable[GroupAdminMapping]): Future[Iterable[(String, String)]] = {
      Future.sequence(
        mappings.map {
          case GroupAdminMapping(_, gid, aid) =>
            for {
              a <- AdminDAO.getOne(aid) if a.nonEmpty
              g <- GroupDAO.getOne(gid) if g.nonEmpty
            } yield (g.get.groupName, a.get.loginName)
        }
      )
    }
    val q = actiRPCO match {
      case Some(actiRPC) =>
        for {
          ms <- q0
          _ <- q1
          _ <- q2
          r1 <- DBIO.from(zipGroupAdmin(ms).flatMap(xs => Future.sequence(xs.map(ys => actiRPC.deleteMemberFromGroup(ys._1, ys._2)))))
          if r1.forall(r => Seq(StatusCodes.NoContent, StatusCodes.NotFound).contains(r.status))
          r2 <- DBIO.from(zipGroupAdmin(mappings).flatMap(xs => Future.sequence(xs.map(ys => actiRPC.addMemberToGroup(ys._1, ys._2)))))
          if r2.forall(r => Seq(StatusCodes.Created, StatusCodes.Conflict).contains(r.status))
        } yield ()
      case None => DBIO.seq(q1, q2)
    }
    db.run(q.transactionally)
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
    // we don't need to add investor group to activiti now
    insert(Group(None, "GlobalInvestorGroup", Groups.GroupTypeInvestor), None)
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
