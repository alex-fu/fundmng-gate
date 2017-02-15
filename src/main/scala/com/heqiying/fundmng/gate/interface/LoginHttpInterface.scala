package com.heqiying.fundmng.gate.interface

import akka.actor.ActorSystem
import com.heqiying.fundmng.gate.api.LoginAPI
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.service.GateApp

class LoginHttpInterface(implicit val app: GateApp, system: ActorSystem) extends LazyLogging {
  val route = new LoginAPI routes
}
