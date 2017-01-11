package com.heqiying.fundmng.gate.api

import javax.ws.rs.Path

import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.dao.AdminDAO
import com.heqiying.fundmng.gate.directives.AuthParams
import com.heqiying.fundmng.gate.model.{ Admin, Accesser }
import io.igl.jwt._
import io.swagger.annotations.{ Api, ApiImplicitParam, ApiImplicitParams, ApiOperation }
import spray.json._

import scala.util.{ Failure, Success }

@Api(value = "Login API", produces = "application/json", protocols = "http")
@Path("/api/v1/")
class LoginAPI extends LazyLogging {
  import scala.concurrent.ExecutionContext.Implicits.global

  val routes = adminLoginRoute

  def buildJWT(accesser: Accesser): String = {
    import com.heqiying.fundmng.gate.model.AccesserJsonSupport._

    val subject = accesser.toJson.compactPrint
    val iat = System.currentTimeMillis() / 1000L
    val exp = iat + 3600 * 12 // 12 hours
    val jwt = new DecodedJwt(Seq(
      Alg(Algorithm.getAlgorithm(AuthParams.JwtAlgo).getOrElse(Algorithm.HS512)),
      Typ("JWT")
    ), Seq(
      Iss(AuthParams.issuer),
      Iat(iat),
      Exp(exp),
      Sub(subject)
    ))
    jwt.encodedAndSigned(AuthParams.salt)
  }

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
          AdminDAO.getOneByLoginName(loginName).map {
            case None => Left("Admin not exist!")
            case Some(admin) =>
              if (admin.password != password) Left("Wrong password!")
              else {
                val adminless = Accesser(admin.loginName, Some(admin.adminName), Some(admin.email), admin.wxid)
                Right(buildJWT(adminless))
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
}
