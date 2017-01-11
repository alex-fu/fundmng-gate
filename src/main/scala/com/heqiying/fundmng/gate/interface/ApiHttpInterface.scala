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

class ApiHttpInterface(implicit system: ActorSystem) extends LazyLogging {
  private[this] val swaggerDocService = new SwaggerDocService(system)

  private[this] val routes = Seq(
    swaggerDocService.docsRoutes,
    new AdminAPI routes,
    new GroupAPI routes,
    new AuthorityAPI routes
  )

  val r0 = routes.reduceLeft { _ ~ _ }
  val r1 = DebuggingDirectives.logRequestResult(("Request Response: ", Logging.InfoLevel))(r0)
  val route = DebuggingDirectives.logRequestResult(showLogs _)(r1)
  def showLogs(request: HttpRequest): RouteResult => Option[LogEntry] = {
    case Complete(response: HttpResponse) =>
      Some(LogEntry(
        s"""request completed. status = ${response.status}, method = ${request.method},
           |path = ${request.uri}, headers = ${response.headers}, encoding = ${response.encoding.value},
           |response length = ${response.entity.contentLengthOption.getOrElse(0L)} bytes"""".stripMargin.toString,
        Logging.InfoLevel
      ))

    case _ =>
      logger.warn("No HttpResponse received!")
      None
  }

}
