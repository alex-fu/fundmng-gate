import com.heqiying.fundmng.gate.common.ProxyConfig
import com.heqiying.fundmng.gate.common.ProxyConfig.ProxyRoute
import com.heqiying.fundmng.gate.common.ProxyConfig.ProxyRouteJsonSupport._
import spray.json._

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


  def checkMatch(uri: String) = {
    val p = "(/api/v1/.+?(/|$))"
    val pattern = s"""$p""".r
    uri match {
      case pattern(path, _*) => println(s"match $path")
      case _ => println("unmatch")
    }
  }

  checkMatch("/api/v1/funds")
  checkMatch("/api/v1/funds/aa")
}
