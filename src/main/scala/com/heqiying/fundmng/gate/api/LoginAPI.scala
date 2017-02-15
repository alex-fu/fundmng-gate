package com.heqiying.fundmng.gate.api

import javax.ws.rs.Path

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.directives.{ AuthDirective, AuthParams }
import com.heqiying.fundmng.gate.service.GateApp
import io.swagger.annotations.{ Api, ApiImplicitParam, ApiImplicitParams, ApiOperation }
import spray.json.DefaultJsonProtocol

import scala.util.{ Failure, Success }

@Api(value = "Login API", produces = "application/json", protocols = "http")
@Path("/api/v1/")
class LoginAPI(implicit app: GateApp) extends LazyLogging {
  case class LoginRequest(loginName: String, password: String)
  object LoginRequestJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
    implicit val jsonFormat = jsonFormat2(LoginRequest.apply)
  }

  val routes = adminLoginRoute

  @Path("/adminLogin")
  @ApiOperation(value = "Admin login", nickname = "admin-login", consumes = "application/json", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "request", value = """{"loginName":"fuyf","password":"fuyf"}""", required = true, dataType = "string", paramType = "body")
  ))
  def adminLoginRoute = path("api" / "v1" / "adminLogin") {
    import LoginRequestJsonSupport._
    post {
      entity(as[LoginRequest]) { req =>
        onComplete(app.adminLogin(req.loginName, req.password)) {
          case Success(Right(accesser)) =>
            val jwt = AuthDirective.buildJWT(accesser)
            setCookie(HttpCookie(AuthParams.cookieName, value = jwt, httpOnly = true, domain = AuthParams.cookieDomain, path = AuthParams.cookiePath)) {
              complete(HttpResponse(StatusCodes.OK, entity = s"""{"jwt": $jwt}"""))
            }
          case Success(Left(x)) => complete(HttpResponse(StatusCodes.BadRequest, entity = x))
          case Failure(e) =>
            logger.error(s"Admin ${req.loginName} login failed: $e")
            complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
        }
      } ~ {
        logger.error(s"login failed! Errors: wrong argument")
        complete(HttpResponse(StatusCodes.BadRequest))
      }
    }
  }

  @Path("/funds/{fundid}/testForForward")
  @ApiOperation(value = "Test for forward", nickname = "test-for-forward", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "fundid", value = "", required = true, dataType = "integer", paramType = "path")
  ))
  def forwardRoute = {
    complete("yes")
  }

  @Path("/funds")
  @ApiOperation(value = "Test for funds", nickname = "test-for-funds", httpMethod = "GET")
  def getFundsRoute = {
    println("hhhhhhhhhhhhhhhhhhhhere")
    complete("yes")
  }
}
