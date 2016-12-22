package com.heqiying.fundmng.gate.interface

import scala.reflect.runtime.universe._
import akka.actor._
import akka.stream.ActorMaterializer
import com.github.swagger.akka.{HasActorSystem, SwaggerHttpService}
import com.github.swagger.akka.model.Info
import com.heqiying.fundmng.gate.api._

class SwaggerDocService(system: ActorSystem) extends SwaggerHttpService with HasActorSystem {
  override implicit val actorSystem: ActorSystem = system
  override implicit val materializer: ActorMaterializer = ActorMaterializer()
  override val apiDocsPath = "api"              //where you want the swagger-json endpoint exposed
  override val info = Info()                    //provides license and other description details

  override val apiTypes = Seq(
    typeOf[UserAPI]
  )

  val docsRoutes = get {
    path("") {
      pathEndOrSingleSlash {
        getFromResource("swagger-ui/index.html")
      }
    } ~ getFromResourceDirectory("swagger-ui")
  } ~ routes

}
