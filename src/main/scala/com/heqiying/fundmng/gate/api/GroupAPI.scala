package com.heqiying.fundmng.gate.api

import javax.ws.rs.Path

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.dao.{ ActiIdentityRPC, AdminDAO, AuthorityDAO, GroupDAO }
import com.heqiying.fundmng.gate.model.{ Authority, Group, Groups }
import com.heqiying.fundmng.gate.directives.AuthDirective._
import com.heqiying.fundmng.gate.interface.ActivitiInterface
import io.swagger.annotations.{ Api, ApiImplicitParam, ApiImplicitParams, ApiOperation }

import scala.concurrent.Future
import scala.util.{ Failure, Success }

@Api(value = "Group API", produces = "application/json")
@Path("/api/v1/groups")
class GroupAPI(implicit val system: ActorSystem, val mat: ActorMaterializer) extends LazyLogging {
  val routes = getRoute ~ postRoute ~ getXRoute ~ deleteXRoute ~
    getGroupAdminsRoute ~ putGroupAdminsRoute ~
    getGroupAuthoritiesRoute ~ putGroupAuthoritiesRoute ~
    getInvestorGroupAuthoritiesRoute ~ putInvestorGroupAuthoritiesRoute

  @ApiOperation(value = "get groups", nickname = "get-groups", httpMethod = "GET")
  def getRoute = path("api" / "v1" / "groups") {
    import com.heqiying.fundmng.gate.model.GroupJsonSupport._
    get {
      onComplete(GroupDAO.getAll) {
        case Success(r) => complete(r)
        case Failure(e) =>
          logger.error(s"get groups failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
      }
    }
  }

  @ApiOperation(value = "create a new group", nickname = "create-group", consumes = "application/x-www-form-urlencoded", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "groupName", required = true, dataType = "string", paramType = "form", defaultValue = "")
  //    new ApiImplicitParam(name = "groupType", required = true, dataType = "string", paramType = "form", defaultValue = "")
  ))
  def postRoute = path("api" / "v1" / "groups") {
    import com.heqiying.fundmng.gate.model.GroupJsonSupport._
    post {
      formFields('groupName) { groupName =>
        extractAccesser { accesser =>
          val group = Group(None, groupName, Groups.GroupTypeAdmin)
          logger.debug(s"Add new group: ${group.toString}")
          val actiRPC: Option[ActiIdentityRPC] = accesser.map(x => new ActivitiInterface(x.loginName)).map(new ActiIdentityRPC(_))
          onComplete(GroupDAO.insert(group, actiRPC)) {
            case Success(r) => complete(group)
            case Failure(e) =>
              logger.error(s"post groups failed: $e")
              complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
          }
        }
      }
    }
  }

  @Path("/{groupid}")
  @ApiOperation(value = "get group by id", nickname = "get-group-by-id", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "groupId", required = true, dataType = "integer", paramType = "path")
  ))
  def getXRoute = path("api" / "v1" / "groups" / IntNumber) { groupid =>
    import com.heqiying.fundmng.gate.model.GroupJsonSupport._
    get {
      onComplete(GroupDAO.getOne(groupid)) {
        case Success(Some(r)) => complete(r)
        case Success(None) => complete(HttpResponse(StatusCodes.NotFound))
        case Failure(e) =>
          logger.error(s"get group by id $groupid failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
      }
    }
  }

  // Obsolete: don't allow to update group, only can create & delete group
  @Path("/{groupid}")
  @ApiOperation(value = "update group", nickname = "update-group", consumes = "application/x-www-form-urlencoded", httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "groupId", required = true, dataType = "integer", paramType = "path", defaultValue = ""),
    new ApiImplicitParam(name = "groupName", required = true, dataType = "string", paramType = "form", defaultValue = "")
  //    new ApiImplicitParam(name = "groupType", required = true, dataType = "string", paramType = "form", defaultValue = "")
  ))
  def putXRoute = path("api" / "v1" / "groups" / IntNumber) { groupid =>
    put {
      formFields('groupName) { groupName =>
        onComplete(GroupDAO.update(groupid, groupName, Groups.GroupTypeAdmin)) {
          case Success(_) => complete(HttpResponse(StatusCodes.OK))
          case Failure(e) =>
            logger.error(s"update group by id $groupid failed: $e")
            complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
        }
      }
    }
  }

  @Path("/{groupid}")
  @ApiOperation(value = "delete group", nickname = "delete-group", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "groupId", required = true, dataType = "integer", paramType = "path")
  ))
  def deleteXRoute = path("api" / "v1" / "groups" / IntNumber) { groupid =>
    delete {
      extractAccesser { accesser =>
        val actiRPC: Option[ActiIdentityRPC] = accesser.map(x => new ActivitiInterface(x.loginName)).map(new ActiIdentityRPC(_))
        onComplete(GroupDAO.delete(groupid, actiRPC)) {
          case Success(_) => complete(HttpResponse(StatusCodes.NoContent))
          case Failure(e) =>
            logger.error(s"delete group by id $groupid failed: $e")
            complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
        }
      }
    }
  }

  @Path("/{groupid}/admins")
  @ApiOperation(value = "get admins in group", nickname = "get-admins-in-group", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "groupId", required = true, dataType = "integer", paramType = "path")
  ))
  def getGroupAdminsRoute = path("api" / "v1" / "groups" / IntNumber / "admins") { groupid =>
    import com.heqiying.fundmng.gate.model.AdminJsonSupport._

    import scala.concurrent.ExecutionContext.Implicits.global
    get {
      val adminIds = GroupDAO.getAdminsInGroup(groupid)
      val admins = adminIds.flatMap { xs =>
        Future.sequence(xs.map { adminId =>
          AdminDAO.getOne(adminId)
        })
      }.map(_.flatten)

      onComplete(admins) {
        case Success(r) => complete(r)
        case Failure(e) =>
          logger.error(s"get admins in group $groupid failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
      }
    }
  }

  @Path("/{groupid}/admins")
  @ApiOperation(value = "set admins in group", nickname = "set-admins-in-group", consumes = "application/json", httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "groupId", required = true, dataType = "integer", paramType = "path"),
    new ApiImplicitParam(name = "data", required = true, dataType = "string", paramType = "body", value = "[1,2,3]")
  ))
  def putGroupAdminsRoute = path("api" / "v1" / "groups" / IntNumber / "admins") { groupid =>
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
    import spray.json.DefaultJsonProtocol._
    put {
      entity(as[Seq[Int]]) { adminIds =>
        extractAccesser { accesser =>
          val actiRPC: Option[ActiIdentityRPC] = accesser.map(x => new ActivitiInterface(x.loginName)).map(new ActiIdentityRPC(_))
          onComplete(GroupDAO.postAdminsInGroup(groupid, adminIds, actiRPC)) {
            case Success(r) => complete(HttpResponse(StatusCodes.OK))
            case Failure(e) =>
              logger.error(s"post admins to group $groupid failed: $e")
              complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
          }
        }
      }
    }
  }

  @Path("/{groupid}/authorities")
  @ApiOperation(value = "get authorities on group", nickname = "get-authorities-on-group", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "groupId", required = true, dataType = "integer", paramType = "path")
  ))
  def getGroupAuthoritiesRoute = path("api" / "v1" / "groups" / IntNumber / "authorities") { groupid =>
    import com.heqiying.fundmng.gate.model.AuthorityJsonSupport._
    import scala.concurrent.ExecutionContext.Implicits.global
    get {
      val authorityNames: Future[Seq[String]] = AuthorityDAO.getAuthoritiesInGroup(groupid)
      val authorities = authorityNames.flatMap { xs =>
        Future.sequence(xs.map { authName =>
          AuthorityDAO.getOne(authName)
        })
      }.map(_.flatten)

      onComplete(authorities) {
        case Success(r) => complete(r)
        case Failure(e) =>
          logger.error(s"get authorities on group $groupid failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
      }
    }
  }

  @Path("/{groupid}/authorities")
  @ApiOperation(value = "set authorities on group", nickname = "set-authorities-on-group", consumes = "application/json", httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "groupId", required = true, dataType = "integer", paramType = "path"),
    new ApiImplicitParam(name = "data", required = true, dataType = "string", paramType = "body", value = """["AdminManage", "FundManage"]""")
  ))
  def putGroupAuthoritiesRoute = path("api" / "v1" / "groups" / IntNumber / "authorities") { groupid =>
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
    import spray.json.DefaultJsonProtocol._
    put {
      entity(as[Seq[String]]) { authorityNames =>
        onComplete(AuthorityDAO.postAuthoritiesInGroup(groupid, authorityNames)) {
          case Success(r) => complete(HttpResponse(StatusCodes.OK))
          case Failure(e) =>
            logger.error(s"post authorities on group $groupid failed: $e")
            complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
        }
      }
    }
  }

  @Path("/investorgroup/authorities")
  @ApiOperation(value = "get authorities on investorgroup", nickname = "get-authorities-on-investorgroup", httpMethod = "GET")
  def getInvestorGroupAuthoritiesRoute = path("api" / "v1" / "groups" / "investorgroup" / "authorities") {
    import com.heqiying.fundmng.gate.model.AuthorityJsonSupport._
    import scala.concurrent.ExecutionContext.Implicits.global
    get {
      val authorityNames: Future[Seq[String]] =
        for {
          investorGroup <- GroupDAO.getOrCreateInvestorGroup()
          if investorGroup.nonEmpty
          authorityNames <- AuthorityDAO.getAuthoritiesInGroup(investorGroup.get.groupId.get)
        } yield authorityNames
      val authorities = authorityNames.flatMap { xs =>
        Future.sequence(xs.map { authName =>
          AuthorityDAO.getOne(authName)
        })
      }.map(_.flatten)

      onComplete(authorities) {
        case Success(r) => complete(r)
        case Failure(e) =>
          logger.error(s"get authorities on investor group failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
      }
    }
  }

  @Path("/investorgroup/authorities")
  @ApiOperation(value = "set authorities on investorgroup", nickname = "set-authorities-on-investorgroup", consumes = "application/json", httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "data", required = true, dataType = "string", paramType = "body", value = """["FundQuery"]""")
  ))
  def putInvestorGroupAuthoritiesRoute = path("api" / "v1" / "groups" / "investorgroup" / "authorities") {
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
    import spray.json.DefaultJsonProtocol._
    import scala.concurrent.ExecutionContext.Implicits.global
    put {
      entity(as[Seq[String]]) { authorityNames =>
        val f = for {
          investorGroup <- GroupDAO.getOrCreateInvestorGroup()
          if investorGroup.nonEmpty
          r <- AuthorityDAO.postAuthoritiesInGroup(investorGroup.get.groupId.get, authorityNames)
        } yield r
        onComplete(f) {
          case Success(r) => complete(HttpResponse(StatusCodes.OK))
          case Failure(e) =>
            logger.error(s"post authorities on investor group failed: $e")
            complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
        }
      }
    }
  }
}
