package com.heqiying.fundmng.gate

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.heqiying.fundmng.gate.common.{ AppConfig, LazyLogging }
import com.heqiying.fundmng.gate.interface.{ ApiHttpInterface, RouteHttpInterface }

import scala.concurrent.Await
import scala.concurrent.duration._

object FundmngGateMicroService extends App with LazyLogging {
  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val mat = ActorMaterializer()
  implicit val timeout = 10.seconds

  val apiInterface = new ApiHttpInterface()
  val routeInterface = new RouteHttpInterface()

  val routes = apiInterface.route
  //  ~ routeInterface.route
  val httpBindingFuture = Http().bindAndHandle(routes, "0.0.0.0", AppConfig.fundmngGate.admin.port)
  logger.info(s"""Server online at http://0.0.0.0:${AppConfig.fundmngGate.admin.port}/ ...""")

  sys addShutdownHook {
    logger.info(s"""Server will shutdown ...""")
    Await.ready(httpBindingFuture.map(_.unbind()), timeout)
  }

  try {
    val stream = getClass.getResourceAsStream("/issue.txt")
    val text = scala.io.Source.fromInputStream(stream).mkString
    stream.close()
    println(text)
  } finally {}
}
