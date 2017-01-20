package com.heqiying.fundmng.gate.interface

import java.time.LocalDateTime

import akka.event.Logging
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.directives.{ DebuggingDirectives, LogEntry }
import akka.http.scaladsl.server.{ Route, RouteResult }
import com.heqiying.fundmng.gate.dao.AccessRecordDAO
import com.heqiying.fundmng.gate.directives.AuthDirective._
import com.heqiying.fundmng.gate.model.{ AccessRecord, Accesser }

object HttpLoggerInterface {
  val logRoute = { route: Route =>
    (extractJWT & extractAccesser & extractUri & extractMethod & extractClientIP) { (jwt, accesser, uri, method, clientip) =>
      DebuggingDirectives.logRequestResult(showLogs(uri, method, clientip, accesser, jwt) _) {
        DebuggingDirectives.logRequestResult(("Request Response", Logging.InfoLevel)) {
          route
        }
      }
    }
  }

  private[this] def saveAccessLog(uri: Uri, method: HttpMethod, clientip: RemoteAddress, accesser: Option[Accesser], jwt: Option[String], response: Option[HttpResponse]) = {
    if (uri.path.toString().startsWith("/api/")) {
      AccessRecordDAO.insert(AccessRecord(None, LocalDateTime.now.toString, uri.path.toString(),
        method.value, accesser.map(_.loginName), clientip.toOption.map(_.getHostAddress), None,
        response.map(_.status.intValue()).getOrElse(404)))
    }
  }

  private[this] def showLogs(uri: Uri, method: HttpMethod, clientip: RemoteAddress, accesser: Option[Accesser], jwt: Option[String])(request: HttpRequest): RouteResult => Option[LogEntry] = {
    case Complete(response: HttpResponse) =>
      saveAccessLog(uri, method, clientip, accesser, jwt, Some(response))

      Some(LogEntry(
        s"request completed. status = ${response.status}, method = ${request.method}, path = ${request.uri}, " +
          s"response_headers = ${response.headers}, " +
          s"response length = ${response.entity.contentLengthOption.getOrElse(0L)} bytes",
        Logging.InfoLevel
      ))

    case _ =>
      saveAccessLog(uri, method, clientip, accesser, jwt, None)

      Some(LogEntry(
        s"request missed. method = ${request.method}, path = ${request.uri}",
        Logging.WarningLevel
      ))
  }
}
