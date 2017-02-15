package com.heqiying.fundmng.gate.service

import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.dao.AuthorityDAO
import com.heqiying.fundmng.gate.model.Authority
import com.heqiying.fundmng.gate.utils.QueryParam

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

trait AuthorityService extends LazyLogging {
  def updateAuthoritiesFromFile(filepathOption: Future[Option[String]]) = {
    import spray.json._
    import com.heqiying.fundmng.gate.model.AuthorityJsonSupport._
    val authorities = filepathOption.map {
      case Some(filepath) => Some(Source.fromFile(filepath).mkString.parseJson.convertTo[Seq[Authority]])
      case _ => None
    }
    authorities.flatMap {
      case Some(x) => AuthorityDAO.upsert(x).map(x => Right(x))
      case _ => Future(Left("please specify authorities file!"))
    }
  }

  def getAuthorities(qp: QueryParam) = {
    AuthorityDAO.get(qp)
  }
}
