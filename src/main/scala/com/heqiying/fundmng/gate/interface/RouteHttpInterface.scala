package com.heqiying.fundmng.gate.interface

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.heqiying.fundmng.gate.common.{LazyLogging, ProxyConfig}
import com.heqiying.fundmng.gate.dao.{AuthorityDAO, GroupDAO}
import com.heqiying.fundmng.gate.directives.AuthDirective._
import com.heqiying.fundmng.gate.directives.ExtendPathMatchers
import com.heqiying.fundmng.gate.model.Accesser

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class RouteHttpInterface(implicit system: ActorSystem, mat: ActorMaterializer) extends LazyLogging {
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
          case _ => get
        }.foldLeft(Directive.Empty)(_ | _)
      }
      policies.map {
        case (k, v) => path(ExtendPathMatchers.separateOnSlashes(k)) & methods(v)
      }.foldRight(forbiddenDirective)(_ | _)
    }

    authenticateJWT { accesser =>
      val policies = accesser match {
        case Some(x) =>
          val r = for {
            groupids <- GroupDAO.getGroupsForAdmin(x.id)
            authorityNames <- Future.sequence(groupids.map(AuthorityDAO.getAuthoritiesInGroup)).map(_.flatten.toSet)
            authorities <- Future.sequence(authorityNames.map(AuthorityDAO.getOne)).map(_.flatten.toSeq)
            expressions <- Future(authorities.flatMap(_.expressions).
              foldLeft(Map.empty[String, Set[String]])((m, x) => m + (x.pathExpression -> x.httpMethods.toSet)))
          } yield expressions
          Some(r)
        case _ => None
      }
      extractUri { uri =>
        policies match {
          case Some(x) =>
            forwardPolicy(Await.result(policies.get, 10.seconds)) {
              proxy(uri.path.toString(), accesser.get)
            }
          case _ => proxy(uri.path.toString(), accesser.get)
        }
      }
    }
  }

  def proxy(uri: String, accesser: Accesser) = {
    val r = ProxyConfig.getProxyServer(uri)
    r match {
      case Some((host, port)) => proxyTo(host, port, accesser)
      case None => complete(HttpResponse(StatusCodes.NotFound))
    }
  }

  def proxyTo(host: String, port: Int, accesser: Accesser) = Route { (context: RequestContext) =>
    val request = context.request
    val flow = Http(system).outgoingConnection(host, port)
    val handler = Source.single(context.request)
      .map(r => r.removeHeader("Timeout-Access").
        addHeader(RawHeader("X-AccesserID", accesser.id.toString)).
        addHeader(RawHeader("X-AccesserLoginName", accesser.loginName)).
        addHeader(RawHeader("X-AccesserName", accesser.name.getOrElse(""))).
        addHeader(RawHeader("X-AccesserEmail", accesser.email.getOrElse(""))).
        addHeader(RawHeader("X-AccesserWxID", accesser.wxid.getOrElse(""))).
        addHeader(RawHeader("X-AccesserType", accesser.groupType)))
      .via(flow)
      .runWith(Sink.head)
      .flatMap(context.complete(_))
    handler
  }
}
