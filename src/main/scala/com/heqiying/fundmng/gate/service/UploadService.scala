package com.heqiying.fundmng.gate.service

import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.dao.UploadsDAO
import com.heqiying.fundmng.gate.model.Uploads
import com.heqiying.fundmng.gate.utils.QueryParam

trait UploadService extends LazyLogging {
  def addUploads(u: Uploads) = {
    logger.info(s"add new uploads $u")
    UploadsDAO.insert(u)
  }

  def getUploads(qp: QueryParam) = {
    UploadsDAO.get(qp)
  }
}
