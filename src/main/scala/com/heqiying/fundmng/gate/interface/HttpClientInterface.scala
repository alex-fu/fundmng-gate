package com.heqiying.fundmng.gate.interface

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ Authorization, BasicHttpCredentials }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Flow, Sink, Source }
import com.heqiying.fundmng.gate.common.{ AppConfig, LazyLogging }

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.util.Try

trait HttpClientInterface extends LazyLogging {
  implicit val system: ActorSystem
  implicit val mat: ActorMaterializer
  implicit val ec = system.dispatcher
  def poolFlow: Flow[(HttpRequest, Int), (Try[HttpResponse], Int), HostConnectionPool]
  val auth: Option[Authorization]
  val baseUri: String

  def issueRequest(
    method: HttpMethod,
    relativeUri: String,
    headers: Seq[HttpHeader] = Nil,
    entity: RequestEntity = HttpEntity.Empty
  ): Future[HttpResponse] = {
    val uri = baseUri + (if (relativeUri.startsWith("/")) relativeUri else "/" + relativeUri)
    val req = HttpRequest(method = method, uri = uri, headers = headers ++ auth.toSeq, entity = entity)
    Source.single(req -> 0).via(poolFlow).runWith(Sink.head).map(_._1.get)
  }

  def get(relativeUri: String, headers: Seq[HttpHeader] = Nil) = {
    issueRequest(HttpMethods.GET, relativeUri, headers)
  }

  def post(relativeUri: String, headers: Seq[HttpHeader] = Nil, entity: RequestEntity = HttpEntity.Empty) = {
    issueRequest(HttpMethods.POST, relativeUri, headers, entity)
  }

  def put(relativeUri: String, headers: Seq[HttpHeader] = Nil, entity: RequestEntity = HttpEntity.Empty) = {
    issueRequest(HttpMethods.PUT, relativeUri, headers, entity)
  }

  def delete(relativeUri: String, headers: Seq[HttpHeader] = Nil, entity: RequestEntity = HttpEntity.Empty) = {
    issueRequest(HttpMethods.DELETE, relativeUri, headers, entity)
  }
}

class ActivitiInterface(username: String, password: Option[String] = None)(implicit val system: ActorSystem, val mat: ActorMaterializer)
    extends HttpClientInterface {
  override val poolFlow = Http().cachedHostConnectionPool[Int](AppConfig.fundmngGate.activiti.host, AppConfig.fundmngGate.activiti.port)
  override val auth: Option[Authorization] = Some(Authorization(BasicHttpCredentials(username, password.getOrElse(AppConfig.fundmngGate.activiti.defaultPassword))))
  override val baseUri: String = AppConfig.fundmngGate.activiti.baseUri
}