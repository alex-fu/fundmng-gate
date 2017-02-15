package com.heqiying.fundmng.gate.service

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

class ManageApp(implicit val system: ActorSystem, val mat: ActorMaterializer) extends AdminService
