package com.heqiying.fundmng.gate.api

import java.nio.file.{ Paths }
import javax.ws.rs.Path

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.model.User
import com.heqiying.fundmng.gate.directives.FileDirective._
import io.swagger.annotations.{ Api, ApiImplicitParam, ApiImplicitParams, ApiOperation }

@Api(value = "User API", produces = "application/json")
@Path("/api/v1/user")
class UserAPI(implicit system: ActorSystem) extends LazyLogging {

  import com.heqiying.fundmng.gate.model.UserJsonSupport._

  val routes = getRoute ~ postRoute ~ getUserIdentityRoute

  @ApiOperation(value = "get user", nickname = "get-user", httpMethod = "GET")
  def getRoute = path("api" / "v1" / "user") {
    get {
      val user = User("fuyf", "fuyf")
      complete(user)
    }
  }

  @ApiOperation(value = "create or update user", nickname = "create-or-update-user", consumes = "multipart/form-data", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "username", required = true, dataType = "string", paramType = "form", defaultValue = ""),
    new ApiImplicitParam(name = "password", required = true, dataType = "string", paramType = "form", defaultValue = ""),
    new ApiImplicitParam(name = "identity", required = true, dataType = "file", paramType = "form", defaultValue = "")
  ))
  def postRoute = path("api" / "v1" / "user") {
    post {
      val user = User("pass", "pass")
      complete(user)
    }
  }

  @Path("/identity/{userid}")
  @ApiOperation(value = "get user identity", nickname = "get-user-identity", produces = "text/plain", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "userid", required = true, dataType = "integer", paramType = "path")
  ))
  def getUserIdentityRoute = path("api" / "v1" / "user" / "identity" / IntNumber) { userid =>
    get {
      logger.info(s"get userid ${userid}'s identity")
      //      val f = Paths.get("/home/fuyf/github/mychart/idea.png")
      //      val str = Source.fromFile("/home/fuyf/github/mychart/idea.png").mkString
      //      val data = Multipart.FormData(
      //        Multipart.FormData.BodyPart.fromPath("foo", ContentTypes.`application/octet-stream`, f)
      //      )
      val path = "/home/fuyf/github/mychart/idea.png"
      downloadFile(path)
    }
  }
}
