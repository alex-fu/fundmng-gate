package com.heqiying.fundmng.gate.common

import com.typesafe.scalalogging.{LazyLogging => LL}


trait LazyLogging extends LL {
  lazy val isSqlDebugEnabled = AppConfig.config.getBoolean("logs.sql.debug")

  def sqlDebug(s: String) = {
    if (isSqlDebugEnabled) {
      logger.debug(s)
    } else {}
  }
}