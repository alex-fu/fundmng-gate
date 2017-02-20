package com.heqiying.fundmng.gate.dao

import com.heqiying.fundmng.gate.model.{ DBSchema, UploadsTable }
import slick.lifted.TableQuery

object UploadsDAO extends CommonDAO[UploadsTable#TableElementType, UploadsTable] {
  override val tableQ: TableQuery[UploadsTable] = DBSchema.uploads

  override val pk: String = "uuid"
}
