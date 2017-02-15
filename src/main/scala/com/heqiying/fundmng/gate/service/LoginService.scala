package com.heqiying.fundmng.gate.service

import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.dao.AdminDAO
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import com.heqiying.fundmng.gate.model.{ Accesser, Groups }

import scala.concurrent.ExecutionContext.Implicits.global

trait LoginService extends LazyLogging {
  def adminLogin(loginName: String, password: String) = {
    AdminDAO.getOne(loginName).map {
      case None => Left("Admin not exist!")
      case Some(admin) =>
        if (admin.password != password) Left("Wrong password!")
        else {
          val accesser = Accesser(admin.loginName, Some(admin.adminName), Some(admin.email), admin.wxid, Groups.GroupTypeAdmin)
          Right(accesser)
        }
    }
  }
}
