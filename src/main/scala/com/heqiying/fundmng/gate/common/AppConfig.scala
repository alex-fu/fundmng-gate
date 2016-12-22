package com.heqiying.fundmng.gate.common

import com.heqiying.fundmng.gate.common.AppConfig.{AdminConfig, ApiConfig, RDSConfig}
import com.typesafe.config.ConfigFactory

case class AppConfig(admin: AdminConfig, api: ApiConfig, rds: RDSConfig)

object AppConfig {
  import com.typesafe.config.Config
  import com.heqiying.konfig.Konfig._

  case class AdminConfig(name: String, port: Int)
  case class ApiConfig(security: SecurityConfig, `private`: PrivateConfig)
  case class SecurityConfig(authentication: Boolean, userSalt: String)
  case class PrivateConfig(allowAllAddress: Boolean, allowedAddress: List[String])
  case class RDSConfig(`type`: String)

  def fromConfig(config: Config): AppConfig = config.read[AppConfig]("fundmng-gate")

  val config = ConfigFactory.load()
  val fundmngGate: AppConfig = fromConfig(config)
}
