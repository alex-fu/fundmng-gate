package com.heqiying.fundmng.gate.utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

case class QueryParam(sort: Option[String], page: Option[Int], size: Option[Int], q: Option[String])

case class QueryResult[E](content: Seq[E], totalElements: Int, totalPages: Int, numberOfElements: Int,
  number: Int, size: Int, first: Boolean, last: Boolean)

object QueryResultJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  def queryResultJsonFormat[E <% ToResponseMarshallable: spray.json.JsonFormat]() = jsonFormat8(QueryResult.apply[E])
}
