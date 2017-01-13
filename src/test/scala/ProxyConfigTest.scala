import com.heqiying.fundmng.gate.common.ProxyConfig
import com.heqiying.fundmng.gate.common.ProxyConfig.ProxyRoute
import com.heqiying.fundmng.gate.common.ProxyConfig.ProxyRouteJsonSupport._
import spray.json._

import scala.util.matching.Regex

object ProxyConfigTest extends App {
  val proxyRoutes = Seq(
    ProxyConfig.ProxyRoute("localhost", 7002, Seq("/api/v1/funds", "/api/v1/company")),
    ProxyConfig.ProxyRoute("localhost", 7003, Seq("/api/v1/investors", "/api/v1/boughtRecords"))
  )

  println(proxyRoutes.toJson.prettyPrint)

  val r = for {
    proxyRoute <- proxyRoutes
    ProxyRoute(host, port, uris) = proxyRoute
    uri <- uris
  } yield uri -> (host, port)

  println(r.toMap)

  def matchPattern(p: Regex, uri: String): Option[String] = {
    uri match {
      case p(x) => Some(x)
      case _ => None
    }
  }

  def getMatch(uri: String): Seq[String] = {
    val patternStrings = Seq("(/api/v1/.+?)/.*", "(.*)")
    val patterns = patternStrings.map(x => s"""$x""".r)
    patterns.flatMap(matchPattern(_, uri))
  }

  def checkMatch(uri: String): Unit = {
    getMatch(uri) match {
      case s@(x :: xs) => println(s"matched on ${s.mkString(";")}")
      case _ => println("unmatch")
    }
  }

  checkMatch("/api/v1/funds")
  checkMatch("/api/v1/funds/aa")
  checkMatch("/api/v1/funds/a/b")
  checkMatch("/api/v1/funds/a/b/c")
}
