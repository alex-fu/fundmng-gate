package com.heqiying.fundmng.gate.dao

import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.model.{ AccessRecords, DBSchema }

object AccessRecordDAO extends CommonDAO[AccessRecords#TableElementType, AccessRecords] with LazyLogging {
  override val tableQ = DBSchema.accessRecords
  override val pk = "id"
}
