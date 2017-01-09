package com.heqiying.fundmng.gate.api

import javax.ws.rs.Path

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.dao.{ AdminDAO, GroupDAO }
import com.heqiying.fundmng.gate.model.{ Admin, AdminID, Group }
import io.swagger.annotations.{ Api, ApiImplicitParam, ApiImplicitParams, ApiOperation }

import scala.concurrent.Future
import scala.util.{ Failure, Success }

@Api(value = "Group API", produces = "application/json")
@Path("/api/v1/groups")
class GroupAPI(implicit system: ActorSystem) extends LazyLogging {
  val routes = getRoute ~ postRoute ~ getXRoute ~ putXRoute ~ deleteXRoute ~ getGroupAdminsRoute ~ postGroupAdminsRoute

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
    new ApiImplicitParam(name = "groupName", required = true, dataType = "string", paramType = "form", defaultValue = ""),
    new ApiImplicitParam(name = "groupType", required = true, dataType = "string", paramType = "form", defaultValue = "")
  ))
  def postRoute = path("api" / "v1" / "groups") {
    import com.heqiying.fundmng.gate.model.GroupJsonSupport._
    post {
      formFields(('groupName, 'groupType)) { (groupName, groupType) =>
        val group = Group(None, groupName, groupType)
        logger.debug(s"Add new group: ${group.toString}")
        onComplete(GroupDAO.insert(group)) {
          case Success(r) => complete(group)
          case Failure(e) =>
            logger.error(s"post groups failed: $e")
            complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
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
          logger.error(s"get group by id($groupid) failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
      }
    }
  }

  @Path("/{groupid}")
  @ApiOperation(value = "update group", nickname = "update-group", consumes = "application/x-www-form-urlencoded", httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "groupId", required = true, dataType = "integer", paramType = "path", defaultValue = ""),
    new ApiImplicitParam(name = "groupName", required = true, dataType = "string", paramType = "form", defaultValue = ""),
    new ApiImplicitParam(name = "groupType", required = true, dataType = "string", paramType = "form", defaultValue = "")
  ))
  def putXRoute = path("api" / "v1" / "groups" / IntNumber) { groupid =>
    put {
      formFields(('groupName, 'groupType)) { (groupName, groupType) =>
        onComplete(GroupDAO.update(groupid, groupName, groupType)) {
          case Success(_) => complete(HttpResponse(StatusCodes.OK))
          case Failure(e) =>
            logger.error(s"update group by id failed: $e")
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
      onComplete(GroupDAO.delete(groupid)) {
        case Success(_) => complete(HttpResponse(StatusCodes.NoContent))
        case Failure(e) =>
          logger.error(s"delete group by id failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
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
          logger.error(s"get admins in group failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
      }
    }
  }

  @Path("/{groupid}/admins")
  @ApiOperation(value = "get admins in group", nickname = "get-admins-in-group", consumes = "application/json", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "groupId", required = true, dataType = "integer", paramType = "path"),
    new ApiImplicitParam(name = "data", required = true, dataType = "string", paramType = "body", value = "[{\"adminId\": 1},{\"adminId\": 2},{\"adminId\": 3}]")
  ))
  def postGroupAdminsRoute = path("api" / "v1" / "groups" / IntNumber / "admins") { groupid =>
    import com.heqiying.fundmng.gate.model.AdminIDJsonSupport._
    post {
      entity(as[Seq[AdminID]]) { adminIds =>
        onComplete(GroupDAO.postAdminsInGroup(groupid, adminIds)) {
          case Success(r) => complete(HttpResponse(StatusCodes.OK))
          case Failure(e) =>
            logger.error(s"post admins to group failed: $e")
            complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
        }
      }
    }
  }
}
