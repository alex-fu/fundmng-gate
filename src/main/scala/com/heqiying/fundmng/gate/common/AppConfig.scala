package com.heqiying.fundmng.gate.common

import com.heqiying.fundmng.gate.common.AppConfig._
import com.typesafe.config.ConfigFactory

case class AppConfig(admin: AdminConfig, api: ApiConfig, route: RouteConfig, activiti: ActivitiConfig, rds: RDSConfig)

object AppConfig {
  import com.typesafe.config.Config
  import com.heqiying.konfig.Konfig._

  case class AdminConfig(name: String, port: Int)
  case class ApiConfig(security: SecurityConfig, authorization: Boolean, `private`: PrivateConfig)
  case class SecurityConfig(authentication: Boolean, algo: String, userSalt: String, domain: String)
  case class PrivateConfig(allowAllAddress: Boolean, allowedAddresses: List[String])
  case class RouteConfig(patterns: List[String])
  case class ActivitiConfig(host: String, port: Int, baseUri: String, defaultPassword: String, dummyUser: String, dummyPassword: String)
  case class RDSConfig(`type`: String)

  def fromConfig(config: Config): AppConfig = config.read[AppConfig]("fundmng-gate")

  lazy val config = ConfigFactory.load()
  lazy val fundmngGate: AppConfig = fromConfig(config)
}
