package com.heqiying.fundmng.gate.common

import com.heqiying.fundmng.gate.common.AppConfig.{ AdminConfig, ApiConfig, RDSConfig, RouteConfig }
import com.typesafe.config.ConfigFactory

case class AppConfig(admin: AdminConfig, api: ApiConfig, route: RouteConfig, rds: RDSConfig)

object AppConfig {
  import com.typesafe.config.Config
  import com.heqiying.konfig.Konfig._

  case class AdminConfig(name: String, port: Int)
  case class ApiConfig(security: SecurityConfig, `private`: PrivateConfig)
  case class SecurityConfig(authentication: Boolean, algo: String, userSalt: String, domain: String)
  case class PrivateConfig(allowAllAddress: Boolean, allowedAddresses: List[String])
  case class RouteConfig(patterns: List[String])
  case class RDSConfig(`type`: String)

  def fromConfig(config: Config): AppConfig = config.read[AppConfig]("fundmng-gate")

  lazy val config = ConfigFactory.load()
  lazy val fundmngGate: AppConfig = fromConfig(config)
}
