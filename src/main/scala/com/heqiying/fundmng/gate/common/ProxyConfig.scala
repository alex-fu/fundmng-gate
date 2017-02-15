package com.heqiying.fundmng.gate.common

import spray.json._

import scala.io.Source
import scala.util.Try
import scala.util.matching.Regex

object ProxyConfig extends LazyLogging {
  private[this] val patternStrings = AppConfig.fundmngGate.route.patterns
  private[this] val patterns = patternStrings.map(x => s"""$x""".r)

  case class ProxyRoute(serviceHost: String, servicePort: Int, pathPrefixes: Seq[String])

  object ProxyRouteJsonSupport extends DefaultJsonProtocol {
    implicit def proxyRouteJsonFormat = jsonFormat3(ProxyRoute.apply)
  }

  private[this] val rawProxyRoutes = {
    val stream = getClass.getResourceAsStream("/proxyRoutes.json")
    import ProxyRouteJsonSupport._
    val proxyRoutes = Source.fromInputStream(stream).mkString.replaceAll("\n", "").parseJson.convertTo[Seq[ProxyRoute]]
    stream.close()

    proxyRoutes
  }

  private[this] val proxyRoutes: Map[String, (String, Int)] = {
    val r = for {
      ProxyRoute(host, port, uris) <- rawProxyRoutes
      uri <- uris
    } yield uri -> (host, port)
    r.toMap
  }

  def getProxyServer(uri: String): Option[(String, Int)] = {
    getUriKey(uri).flatMap(k => proxyRoutes.get(k)).headOption
  }

  private[this] def getUriKey(uri: String): Seq[String] = {
    def matchPattern(p: Regex, uri: String): Option[String] = {
      uri match {
        case p(x) => Some(x)
        case _ => None
      }
    }
    patterns.flatMap(matchPattern(_, uri))
  }

  def debugRawProxyRoute() = {
    def _debugRawProxyRoute(r: Seq[ProxyRoute]) = {
      import ProxyRouteJsonSupport._
      logger.debug(r.toJson.prettyPrint)
    }
    _debugRawProxyRoute(rawProxyRoutes)
  }

  def debugProxyRoute() = {
    def _debugProxyRoute(r: Map[String, (String, Int)]) = {
      def mkString(r: Map[String, (String, Int)]) = {
        val logsb = new StringBuilder
        logsb.append("\n" + "-" * 60 + "\nProxy Routes:\n")
        logsb.append("-" * 60 + "\n")
        r.foreach {
          case (key, serverinfo) =>
            logsb.append(key)
            logsb.append(" => ")
            logsb.append(serverinfo)
            logsb.append("\n")
        }
        logsb.append("-" * 60)
        logsb.mkString
      }
      logger.debug(mkString(r))
    }
    _debugProxyRoute(proxyRoutes)
  }
}
