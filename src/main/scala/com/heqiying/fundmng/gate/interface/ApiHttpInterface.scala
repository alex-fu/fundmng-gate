package com.heqiying.fundmng.gate.interface

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import com.heqiying.fundmng.gate.api._
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.dao.{ AuthorityDAO, GroupDAO }
import com.heqiying.fundmng.gate.directives.AuthDirective
import com.heqiying.fundmng.gate.model.{ Accesser, Groups }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global

class ApiHttpInterface(implicit system: ActorSystem) extends LazyLogging {
  private[this] val routes = Seq(
    new AdminAPI routes,
    new GroupAPI routes,
    new AuthorityAPI routes,
    new LoginAPI routes
  )

  val r0 = routes.reduceLeft {
    _ ~ _
  }
  val route = extractMethod { method =>
    extractUri { uri =>
      if (uri.path.toString() != "/api/v1/adminLogin" && uri.path.toString() != "/api/v1/investorLogin") {
        AuthDirective.authenticateJWT { accesser: Option[Accesser] =>
          // fundmng-gate API is only accessible for SystemAdmin
          val r = accesser match {
            case Some(x) =>
              logger.info(s"""Authenticated Accesser ${x.loginName}(${x.name.getOrElse("")}) request to ${method.value} ${uri.path.toString()}""")
              val r = for {
                groupids <- GroupDAO.getGroupsForAdmin(x.id)
                groupNames <- Future.sequence(groupids.map(GroupDAO.getOne)).map(_.flatten).map(_.map(_.groupName))
                if groupNames contains Groups.GroupNameSystemAdmin
              } yield x.loginName
              Some(r)
            case _ => None
          }
          r match {
            case Some(f) =>
              Await.ready(f, 10.seconds).value match {
                case Some(Success(_)) => r0
                case _ => AuthDirective.notFoundRoute
              }
            case None => r0
          }
        }
      } else {
        r0
      }
    }
  }
}
