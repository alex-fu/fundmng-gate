package com.heqiying.fundmng.gate.dao

import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.model.{ AccessRecord, DBSchema }
import com.heqiying.fundmng.gate.database.MainDBProfile._
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._

object AccessRecordDAO extends LazyLogging {
  private val accessRecords = DBSchema.accessRecords

  def insert(record: AccessRecord) = {
    val q = accessRecords += record
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

}
