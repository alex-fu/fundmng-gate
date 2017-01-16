package com.heqiying.fundmng.gate.interface

import akka.event.Logging
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LogEntry}
import akka.http.scaladsl.server.{Route, RouteResult}
import com.heqiying.fundmng.gate.dao.AccessRecordDAO
import com.heqiying.fundmng.gate.directives.AuthDirective._
import com.heqiying.fundmng.gate.model.{AccessRecord, Accesser}

object RequestResponseLoggerInterface {
  val logRoute = { route: Route =>
    (extractUri & extractMethod & extractClientIP & authenticateJWT) { (uri, method, clientip, accesser) =>
      DebuggingDirectives.logRequestResult(showLogs(uri, method, clientip, accesser) _) {
        DebuggingDirectives.logRequestResult(("Request Response", Logging.InfoLevel)) {
          route
        }
      }
    }
  }

  private[this] def showLogs(uri: Uri, method: HttpMethod, clientip: RemoteAddress, accesser: Option[Accesser])(request: HttpRequest): RouteResult => Option[LogEntry] = {
    case Complete(response: HttpResponse) =>
      // save the access records to database
      if (uri.path.toString().startsWith("/api/")) {
        AccessRecordDAO.insert(AccessRecord(None, System.currentTimeMillis(), uri.path.toString(),
          method.value, accesser.map(_.loginName), clientip.toOption.map(_.toString), None, response.status.intValue()))
      }

      Some(LogEntry(
        s"request completed. status = ${response.status}, method = ${request.method}, path = ${request.uri}, " +
          s"response_headers = ${response.headers}, " +
          s"response length = ${response.entity.contentLengthOption.getOrElse(0L)} bytes",
        Logging.InfoLevel
      ))

    case _ =>
      // save the access records to database
      AccessRecordDAO.insert(AccessRecord(None, System.currentTimeMillis(), uri.path.toString(),
        method.value, accesser.map(_.loginName), clientip.toOption.map(_.toString), None, 404))

      Some(LogEntry(
        s"request missed. method = ${request.method}, path = ${request.uri}",
        Logging.WarningLevel
      ))
  }
}
