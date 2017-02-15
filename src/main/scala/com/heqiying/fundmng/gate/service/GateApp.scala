package com.heqiying.fundmng.gate.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

class GateApp(implicit val system: ActorSystem, val mat: ActorMaterializer)
  extends AdminService with AuthorityService with GroupService with LoginService with RouteService
