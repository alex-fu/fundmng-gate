package com.heqiying.fundmng.gate.api

import javax.ws.rs.Path

import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.dao.AdminDAO
import com.heqiying.fundmng.gate.directives.{ AuthDirective, AuthParams }
import com.heqiying.fundmng.gate.model.{ Accesser, Groups }
import io.swagger.annotations.{ Api, ApiImplicitParam, ApiImplicitParams, ApiOperation }

import scala.util.{ Failure, Success }

@Api(value = "Login API", produces = "application/json", protocols = "http")
@Path("/api/v1/")
class LoginAPI extends LazyLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  val routes = adminLoginRoute

  @Path("/adminLogin")
  @ApiOperation(value = "Admin login", nickname = "admin-login", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "loginName", required = true, dataType = "string", paramType = "form"),
    new ApiImplicitParam(name = "password", required = true, dataType = "string", paramType = "form")
  ))
  def adminLoginRoute = path("api" / "v1" / "adminLogin") {
    post {
      formFields(('loginName, 'password)) { (loginName, password) =>
        onComplete {
          AdminDAO.getOne(loginName).map {
            case None => Left("Admin not exist!")
            case Some(admin) =>
              if (admin.password != password) Left("Wrong password!")
              else {
                val accesser = Accesser(admin.loginName, Some(admin.adminName), Some(admin.email), admin.wxid, Groups.GroupTypeAdmin)
                Right(AuthDirective.buildJWT(accesser))
              }
          }
        } {
          case Success(Right(jwt: String)) =>
            setCookie(HttpCookie(AuthParams.cookieName, value = jwt, httpOnly = true, domain = AuthParams.cookieDomain, path = AuthParams.cookiePath)) {
              complete(HttpResponse(StatusCodes.OK, entity = s"""{"jwt": $jwt}"""))
            }
          case Success(Left(x)) => complete(HttpResponse(StatusCodes.BadRequest, entity = x))
          case Failure(e) =>
            logger.error(s"Admin $loginName login failed: $e")
            complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
        }
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
