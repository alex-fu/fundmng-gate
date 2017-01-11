package com.heqiying.fundmng.gate.interface

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.RouteResult
import akka.http.scaladsl.server.RouteResult._
import com.heqiying.fundmng.gate.api._
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.directives.AuthDirective
import com.heqiying.fundmng.gate.model.Accesser

class ApiHttpInterface(implicit system: ActorSystem) extends LazyLogging {
  private[this] val swaggerDocService = new SwaggerDocService(system)

  private[this] val routes = Seq(
    swaggerDocService.docsRoutes,
    new AdminAPI routes,
    new GroupAPI routes,
    new AuthorityAPI routes,
    new LoginAPI routes
  )

  val r0 = routes.reduceLeft { _ ~ _ }
  val r1 = DebuggingDirectives.logRequestResult(("Request Response", Logging.InfoLevel)) {
    extractMethod { method =>
      extractUri { uri =>
        if (uri.path.toString() != "/api/v1/adminLogin" && uri.path.toString() != "/api/v1/investorLogin") {
          AuthDirective.authenticateJWT { accesser: Option[Accesser] =>
            logger.info(s"""Authenticated Accesser ${accesser.map(_.loginName).getOrElse("")} request to ${method.value} ${uri.path.toString()}""")
            r0
          }
        } else {
          r0
        }
      }
    }
  }
  val route = DebuggingDirectives.logRequestResult(showLogs _)(r1)
  def showLogs(request: HttpRequest): RouteResult => Option[LogEntry] = {
    case Complete(response: HttpResponse) =>
      Some(LogEntry(
        s"request completed. status = ${response.status}, method = ${request.method}, path = ${request.uri}, " +
          s"response_headers = ${response.headers}, " +
          s"response length = ${response.entity.contentLengthOption.getOrElse(0L)} bytes",
        Logging.InfoLevel
      ))

    case _ =>
      Some(LogEntry(
        s"request missed. method = ${request.method}, path = ${request.uri}",
        Logging.WarningLevel
      ))
  }
}
