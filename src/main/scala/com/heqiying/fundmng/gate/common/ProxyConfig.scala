package com.heqiying.fundmng.gate.common

import spray.json._

import scala.io.Source
import scala.util.Try

object ProxyConfig extends LazyLogging {

  case class ProxyRoute(serviceHost: String, servicePort: Int, pathPrefixes: Seq[String])

  object ProxyRouteJsonSupport extends DefaultJsonProtocol {
    implicit def proxyRouteJsonFormat = jsonFormat3(ProxyRoute.apply)
  }

  val rawProxyRoutes = {
    val stream = getClass.getResourceAsStream("/proxyRoutes.json")
    import ProxyRouteJsonSupport._
    val proxyRoutes = Source.fromInputStream(stream).mkString.replaceAll("\n", "").parseJson.convertTo[Seq[ProxyRoute]]
    stream.close()

    proxyRoutes
  }

  val proxyRoutes = {
    val r = for {
      ProxyRoute(host, port, uris) <- rawProxyRoutes
      uri <- uris
    } yield uri -> (host, port)
    r.toMap
  }

  def getProxyServer(uri: String): Option[(String, Int)] = {
    getUriKey(uri).flatMap(k => Try(proxyRoutes(k)).toOption).headOption
  }

  private[this] def getUriKey(uri: String): Seq[String] = {
    Seq(
      extractUriKey(uri),
      uri
    )
  }

  private[this] def extractUriKey(uri: String): String = {
    ""
  }

  def debugRawProxyRoute(r: Seq[ProxyRoute]) = {
    import ProxyRouteJsonSupport._
    logger.debug(r.toJson.prettyPrint)
  }
}
