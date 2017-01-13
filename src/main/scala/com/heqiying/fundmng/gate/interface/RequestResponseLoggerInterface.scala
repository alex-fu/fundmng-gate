package com.heqiying.fundmng.gate.interface

import akka.event.Logging
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.server.{ Route, RouteResult }
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.directives.{ DebuggingDirectives, LogEntry }

object RequestResponseLoggerInterface {
  val logRoute = { route: Route =>
    DebuggingDirectives.logRequestResult(showLogs _) {
      DebuggingDirectives.logRequestResult(("Request Response", Logging.InfoLevel)) {
        route
      }
    }
  }

  private[this] def showLogs(request: HttpRequest): RouteResult => Option[LogEntry] = {
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
