package com.heqiying.fundmng.gate.interface

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import com.heqiying.fundmng.gate.common.{ AppConfig, LazyLogging, ProxyConfig }
import com.heqiying.fundmng.gate.directives.AuthDirective._
import com.heqiying.fundmng.gate.directives.ExtendPathMatchers
import com.heqiying.fundmng.gate.model.Accesser
import com.heqiying.fundmng.gate.service.GateApp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success }

class RouteHttpInterface(implicit val app: GateApp, system: ActorSystem, mat: ActorMaterializer) extends LazyLogging {
  private[this] def debugRoutePolicies(policies: Map[String, Set[String]]) = {
    def mkString(policies: Map[String, Set[String]]) = {
      val logsb = new StringBuilder
      logsb.append("policies: \n")
      policies.foreach {
        case (uri, methods) =>
          logsb.append(uri)
          logsb.append(" => [")
          logsb.append(methods.mkString(", "))
          logsb.append("]\n")
      }
      logsb.mkString
    }
    logger.debug(mkString(policies))
  }

  val route = {
    def forwardPolicy(policies: Map[String, Set[String]]) = {
      debugRoutePolicies(policies)
      def methods(set: Set[String]) = {
        set.map {
          case "GET" => get
          case "POST" => post
          case "PUT" => put
          case "DELETE" => delete
          case "OPTIONS" => options
          case "PATCH" => patch
          case _ => head
        }.foldRight(rejectDirective)(_ | _)
      }
      policies.map {
        case (k, v) => path(ExtendPathMatchers.separateOnSlashes(k)) & methods(v)
      }.foldRight(rejectDirective)(_ | _)
    }

    val needAuthorization = AppConfig.fundmngGate.api.authorization
    authenticateJWT { accesser =>
      extractUri { uri =>
        if (needAuthorization) {
          app.getRoutePolicies(accesser) match {
            case Some(x) =>
              onComplete(x.map(forwardPolicy)) {
                case Success(d) =>
                  d {
                    proxy(uri.path.toString(), accesser)
                  }
                case Failure(e) =>
                  logger.error(s"calculate forward policy failed: $e")
                  reject
              }
            case _ => proxy(uri.path.toString(), accesser)
          }
        } else {
          proxy(uri.path.toString(), accesser)
        }
      }
    }
  }

  def proxy(uri: String, accesser: Option[Accesser]) = {
    val r = ProxyConfig.getProxyServer(uri)
    r match {
      case Some((host, port)) =>
        logger.info(s"Proxy ${accesser.map(_.loginName).getOrElse("")}'s request on $uri to $host:$port")
        proxyTo(host, port, accesser)
      case None => complete(HttpResponse(StatusCodes.NotFound))
    }
  }

  def proxyTo(host: String, port: Int, accesser: Option[Accesser]) = Route { (context: RequestContext) =>
    val flow = Http().outgoingConnection(host, port)
    val handler = Source.single(context.request)
      .map(r => r.removeHeader("Timeout-Access").
        addHeader(RawHeader("X-AccesserLoginName", accesser.map(_.loginName).getOrElse(""))).
        addHeader(RawHeader("X-AccesserName", accesser.flatMap(_.name).getOrElse(""))).
        addHeader(RawHeader("X-AccesserEmail", accesser.flatMap(_.email).getOrElse(""))).
        addHeader(RawHeader("X-AccesserWxID", accesser.flatMap(_.wxid).getOrElse(""))).
        addHeader(RawHeader("X-AccesserType", accesser.map(_.groupType).getOrElse(""))))
      .via(flow)
      .runWith(Sink.head)
      .flatMap(context.complete(_))
    handler.recoverWith {
      case e => context.complete(HttpResponse(StatusCodes.InternalServerError, entity = "Server is in maintain!"))
    }
  }
}
