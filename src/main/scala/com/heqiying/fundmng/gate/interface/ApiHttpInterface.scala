package com.heqiying.fundmng.gate.interface

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.heqiying.fundmng.gate.api._
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.directives.AuthDirective
import com.heqiying.fundmng.gate.model.{ Accesser, Groups }
import com.heqiying.fundmng.gate.service.GateApp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

class ApiHttpInterface(implicit val app: GateApp, val system: ActorSystem, val mat: ActorMaterializer) extends LazyLogging {

  private[this] val uploadRoute = new UploadAPI routes
  private[this] val routes = Seq(
    new AdminAPI routes,
    new GroupAPI routes,
    new AuthorityAPI routes,
    uploadRoute
  )

  val r0 = routes.reduceLeft {
    _ ~ _
  }
  val route = extractMethod { method =>
    extractUri { uri =>
      AuthDirective.authenticateJWT { accesser: Option[Accesser] =>
        // fundmng-gate API is only accessible for SystemAdmin except uploadRoute
        val r = accesser match {
          case Some(x) =>
            logger.info(s"""Authenticated Accesser ${x.loginName}(${x.name.getOrElse("")}) request to ${method.value} ${uri.path.toString()}""")
            val r = for {
              groupNames <- app.getAdminGroupNames(x.loginName)
              if groupNames contains Groups.GroupNameSystemAdmin
            } yield x.loginName
            Some(r)
          case _ => None
        }
        r match {
          case Some(f) =>
            onComplete(f) {
              case Success(_) => r0 ~ AuthDirective.forbiddenRoute
              case _ => uploadRoute ~ AuthDirective.forbiddenRoute
            }
          case None => r0
        }
      }
    }
  }
}