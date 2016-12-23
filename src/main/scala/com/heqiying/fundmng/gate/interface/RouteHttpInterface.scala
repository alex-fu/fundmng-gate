package com.heqiying.fundmng.gate.interface

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.RouteResult._
import com.heqiying.fundmng.gate.common.LazyLogging

class RouteHttpInterface(implicit system: ActorSystem) extends LazyLogging {
  //  private[this] val routes = Seq(
  //    //    new SellerAPI routes
  //  )
  //
  //  val r0 = routes.reduceLeft { _ ~ _ }
  //  val r1 = DebuggingDirectives.logRequestResult("Request Response: ", Logging.InfoLevel)(r0)
  //  val route = DebuggingDirectives.logRequestResult(showLogs _)(r1)
  //  def showLogs(request: HttpRequest): Any => Option[LogEntry] = {
  //    case Complete(response: HttpResponse) => {
  //      Some(LogEntry(s"""request completed. status = ${response.status}, method = ${request.method}, path = ${request.uri}, headers = ${response.headers}, encoding = ${response.encoding.value}, response length = ${response.entity.contentLengthOption.getOrElse(0L)} bytes"""".toString, Logging.InfoLevel))
  //
  //    }
  //    case _ => None
  //  }
}
