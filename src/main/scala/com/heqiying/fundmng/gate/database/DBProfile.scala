package com.heqiying.fundmng.gate.database

import com.typesafe.config.ConfigFactory
import slick.driver.JdbcProfile

import scala.concurrent.duration._

sealed trait DBProfile {
  def profile: JdbcProfile
  def timeoutDuration: FiniteDuration
  def db: slick.jdbc.JdbcBackend.DatabaseDef
}

trait PostgresDB extends DBProfile {
  val profile = slick.driver.PostgresDriver
  val timeoutDuration = 60.seconds
  val db = slick.jdbc.JdbcBackend.Database.forConfig(s"fundmng-gate.rds.postgresql")
}

trait MySQLDB extends DBProfile {
  val profile = slick.driver.MySQLDriver
  val timeoutDuration = 60.seconds
  val db = slick.jdbc.JdbcBackend.Database.forConfig(s"fundmng-gate.rds.mysql")
}

object PostgresDB extends PostgresDB
object MySQLDB extends MySQLDB

object MainDBProfile {
  private val config = ConfigFactory.load
  private val dbType = config.getString("fundmng-gate.rds.type")
  private val pf: DBProfile = dbType match {
    case "postgresql" => PostgresDB
    case "mysql" => MySQLDB
    case _ => MySQLDB
  }
  val profile = pf.profile
  val db = pf.db
  val timeDuration = pf.timeoutDuration
}
