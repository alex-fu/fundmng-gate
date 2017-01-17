package com.heqiying.fundmng.gate.interface

import akka.actor.ActorSystem
import com.heqiying.fundmng.gate.api.LoginAPI
import com.heqiying.fundmng.gate.common.LazyLogging

class LoginHttpInterface(implicit system: ActorSystem) extends LazyLogging {
  val route = new LoginAPI routes
}
