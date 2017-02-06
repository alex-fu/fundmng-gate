package com.heqiying.fundmng.gate.dao

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{ HttpResponse, RequestEntity }
import akka.stream.ActorMaterializer
import com.heqiying.fundmng.gate.common.{ AppConfig, LazyLogging }
import com.heqiying.fundmng.gate.interface.ActivitiInterface
import com.heqiying.fundmng.gate.model.{ Accesser, activiti }

import scala.concurrent.Future
import scala.util.{ Failure, Success }
import scala.concurrent.duration._
import spray.json._

trait ActiHelper extends LazyLogging {
  implicit val system: ActorSystem
  implicit val mat: ActorMaterializer
  implicit val ec = system.dispatcher
  def debugResponse(relativeUri: String, resp: Future[HttpResponse]) = {
    resp onComplete {
      case Success(r) =>
        logger.debug(s"Response from activiti-rest $relativeUri(${r.status}):")
        r.entity.toStrict(10.seconds).map { strict =>
          logger.debug(s"\n${strict.data.utf8String.parseJson.prettyPrint}")
        }
      case Failure(e) =>
        logger.error(s"access to $relativeUri failed: $e")
    }
  }
}

object ActiIdentityRPC {
  private def default(implicit system: ActorSystem, mat: ActorMaterializer) = new ActiIdentityRPC(
    new ActivitiInterface(AppConfig.fundmngGate.activiti.dummyUser, Some(AppConfig.fundmngGate.activiti.dummyPassword))
  )
  def create(accesser: Option[Accesser])(implicit system: ActorSystem, mat: ActorMaterializer): ActiIdentityRPC = {
    accesser.
      map(x => new ActivitiInterface(x.loginName)).
      map(new ActiIdentityRPC(_)).
      getOrElse(default)
  }
}

class ActiIdentityRPC(val acti: ActivitiInterface)(implicit val system: ActorSystem, val mat: ActorMaterializer) extends ActiHelper {
  def createUser(id: String, name: String, email: String): Future[HttpResponse] = {
    import activiti.UserJsonSupport._
    val entity = Marshal(activiti.User(id, name, "", email, Some(AppConfig.fundmngGate.activiti.defaultPassword), None)).to[RequestEntity]
    val uri = "/identity/users"
    entity.flatMap { e =>
      val r = acti.post(uri, Nil, e)
      debugResponse(uri, r)
      r
    }
  }

  def updateUser(id: String, name: String, email: String): Future[HttpResponse] = {
    import activiti.UserJsonSupport._
    val entity = Marshal(activiti.User(id, name, "", email, Some(AppConfig.fundmngGate.activiti.defaultPassword), None)).to[RequestEntity]
    val uri = s"/identity/users/$id"
    entity.flatMap { e =>
      val r = acti.put(uri, Nil, e)
      debugResponse(uri, r)
      r
    }
  }

  def deleteUser(id: String): Future[HttpResponse] = {
    val uri = s"/identity/users/$id"
    val r = acti.delete(uri)
    debugResponse(uri, r)
    r
  }

  def createGroup(id: String, name: String, `type`: String) = {
    import activiti.GroupJsonSupport._
    val entity = Marshal(activiti.Group(id, name, `type`, None)).to[RequestEntity]
    val uri = s"/identity/groups"
    entity.flatMap { e =>
      val r = acti.post(uri, Nil, e)
      debugResponse(uri, r)
      r
    }
  }

  def updateGroup(id: String, name: String, `type`: String) = {
    import activiti.GroupJsonSupport._
    val entity = Marshal(activiti.Group(id, name, `type`, None)).to[RequestEntity]
    val uri = s"/identity/groups/$id"
    entity.flatMap { e =>
      val r = acti.put(uri, Nil, e)
      debugResponse(uri, r)
      r
    }
  }

  def deleteGroup(id: String) = {
    val uri = s"/identity/groups/$id"
    val r = acti.delete(uri)
    debugResponse(uri, r)
    r
  }

  def addMemberToGroup(groupid: String, userid: String) = {
    import activiti.MemberJsonSupport._
    val uri = s"/identity/groups/$groupid/members"
    val entity = Marshal(activiti.Member(userid)).to[RequestEntity]
    entity.flatMap { e =>
      val r = acti.post(uri, Nil, e)
      debugResponse(uri, r)
      r
    }
  }

  def deleteMemberFromGroup(groupid: String, userid: String) = {
    val uri = s"identity/groups/$groupid/members/$userid"
    val r = acti.delete(uri)
    debugResponse(uri, r)
    r
  }
}