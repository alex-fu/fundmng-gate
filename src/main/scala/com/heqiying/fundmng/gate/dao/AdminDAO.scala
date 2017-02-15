package com.heqiying.fundmng.gate.dao

import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.model.{ Admins, DBSchema }

object AdminDAO extends CommonDAO[Admins#TableElementType, Admins] with LazyLogging {
  override val tableQ = DBSchema.admins
  override val pk = "loginName"
}
