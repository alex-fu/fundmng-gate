package com.heqiying.fundmng.gate.service

import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.dao.{ AuthorityDAO, GroupDAO }
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import com.heqiying.fundmng.gate.model.{ Accesser, Groups }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait RouteService extends LazyLogging {
  def getRoutePolicies(accesser: Option[Accesser]) = {
    accesser match {
      case Some(x) if x.groupType == Groups.GroupTypeAdmin =>
        val r = for {
          groupids <- GroupDAO.getGroupsForAdmin(x.loginName)
          authorityNames: Set[String] <- Future.sequence(groupids.map(AuthorityDAO.getAuthoritiesInGroup)).map(_.flatten.toSet)
          authorities <- Future.sequence(authorityNames.map(x => AuthorityDAO.getOne(x))).map(_.flatten.toSeq)
          expressions <- Future(authorities.flatMap(_.expressions).
            foldLeft(Map.empty[String, Set[String]])((m, x) => m + (x.pathExpression -> x.httpMethods.toSet)))
        } yield expressions
        Some(r)
      case Some(x) if x.groupType == Groups.GroupTypeInvestor =>
        val r = for {
          group <- GroupDAO.getOrCreateInvestorGroup()
          if group.nonEmpty
          authorityNames <- AuthorityDAO.getAuthoritiesInGroup(group.get.groupName)
          authorities <- Future.sequence(authorityNames.map(x => AuthorityDAO.getOne(x))).map(_.flatten.toSeq)
          expressions <- Future(authorities.flatMap(_.expressions).
            foldLeft(Map.empty[String, Set[String]])((m, x) => m + (x.pathExpression -> x.httpMethods.toSet)))
        } yield expressions
        Some(r)
      case _ => None
    }
  }
}
