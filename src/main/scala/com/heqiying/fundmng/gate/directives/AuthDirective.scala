package com.heqiying.fundmng.gate.directives

import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import com.heqiying.fundmng.gate.common.{ AppConfig, LazyLogging }
import com.heqiying.fundmng.gate.model.Accesser
import io.igl.jwt._
import spray.json._

import scala.util.{ Failure, Success, Try }

object AuthParams {
  val authenticationRequired = AppConfig.fundmngGate.api.security.authentication
  val JwtAlgo = AppConfig.fundmngGate.api.security.algo
  val salt = AppConfig.fundmngGate.api.security.userSalt

  val issuer = "fundmng"

  val cookieName = "_jwt"
  val cookieDomain = Some(AppConfig.fundmngGate.api.security.domain)
  val cookiePath = Some("/")
}

trait AuthDirective extends LazyLogging {
  val authenticationRequired: Boolean
  val JwtAlgo: String
  val salt: String
  val issuer: String

  def authenticateJWT: Directive1[Option[Accesser]] = {
    if (authenticationRequired) {
      val r = for {
        cookiePair <- optionalCookie("_jwt")
        token <- optionalHeaderValueByName("X-Access-Token")
      } yield {
        val jwtOption = cookiePair.map(_.toCookie()) match {
          case Some(cookie) => Some(cookie.value)
          case _ => token
        }

        jwtOption.flatMap { jwtString =>
          DecodedJwt.validateEncodedJwt(
            jwtString,
            salt,
            Algorithm.getAlgorithm(JwtAlgo).getOrElse(Algorithm.HS512),
            Set(Typ),
            Set(Iss, Iat, Exp, Sub),
            iss = Some(Iss(issuer))
          ) match {
              case Success(jwt: Jwt) =>
                import com.heqiying.fundmng.gate.model.AccesserJsonSupport._
                for {
                  sub <- jwt.getClaim[Sub]
                  exp <- jwt.getClaim[Exp] if exp.value > System.currentTimeMillis() / 1000L
                  accesser <- Try(sub.value.parseJson.convertTo[Accesser]).toOption
                } yield accesser
              case Failure(e) =>
                logger.info(s"jwt validate failed: $e")
                None
            }
        }
      }

      r.flatMap {
        case Some(x) => provide(Some(x))
        case _ => complete(HttpResponse(StatusCodes.Unauthorized))
      }
    } else {
      provide(None)
    }
  }
}

object AuthDirective extends AuthDirective {
  val authenticationRequired = AuthParams.authenticationRequired
  val JwtAlgo = AuthParams.JwtAlgo
  val salt = AuthParams.salt
  val issuer = AuthParams.issuer
}
