package com.heqiying.fundmng.gate.api

import javax.ws.rs.Path

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.dao.{ AdminDAO, GroupDAO }
import com.heqiying.fundmng.gate.model.Admin
import io.swagger.annotations.{ Api, ApiImplicitParam, ApiImplicitParams, ApiOperation }

import scala.concurrent.Future
import scala.util.{ Failure, Success }

@Api(value = "Admin API", produces = "application/json")
@Path("/api/v1/admins")
class AdminAPI(implicit system: ActorSystem) extends LazyLogging {
  val routes = getRoute ~ postRoute ~ getXRoute ~ putXRoute ~ deleteXRoute ~ getAdminGroupsRoute

  @ApiOperation(value = "get admins", nickname = "get-admins", httpMethod = "GET")
  def getRoute = path("api" / "v1" / "admins") {
    import com.heqiying.fundmng.gate.model.AdminJsonSupport._
    get {
      onComplete(AdminDAO.getAll) {
        case Success(admin) => complete(admin)
        case Failure(e) =>
          logger.error(s"post admins failed! Errors: $e")
          complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
      }
    }
  }

  @ApiOperation(value = "create a new admin", nickname = "create-admin", consumes = "application/x-www-form-urlencoded", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "loginName", required = true, dataType = "string", paramType = "form", defaultValue = ""),
    new ApiImplicitParam(name = "password", required = true, dataType = "string", paramType = "form", defaultValue = ""),
    new ApiImplicitParam(name = "adminName", required = true, dataType = "string", paramType = "form", defaultValue = ""),
    new ApiImplicitParam(name = "email", required = true, dataType = "string", paramType = "form", defaultValue = ""),
    new ApiImplicitParam(name = "wxid", required = false, dataType = "string", paramType = "form", defaultValue = "")
  ))
  def postRoute = path("api" / "v1" / "admins") {
    import com.heqiying.fundmng.gate.model.AdminJsonSupport._
    post {
      formFields(('loginName, 'password, 'adminName, 'email, 'wxid.?)) { (loginName, password, adminName, email, wxid) =>
        val admin = Admin(None, loginName, password, adminName, email, wxid, System.currentTimeMillis(), None)
        logger.debug(s"Add new admin: ${admin.toString}")
        onComplete(AdminDAO.insert(admin)) {
          case Success(_) => complete(admin)
          case Failure(e) =>
            logger.error(s"post admins failed! Errors: $e")
            complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
        }
      }
    }
  }

  @Path("/{adminid}")
  @ApiOperation(value = "get admin by id", nickname = "get-admin-by-id", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "adminId", required = true, dataType = "integer", paramType = "path")
  ))
  def getXRoute = path("api" / "v1" / "admins" / IntNumber) { adminid =>
    import com.heqiying.fundmng.gate.model.AdminJsonSupport._
    get {
      onComplete(AdminDAO.getOne(adminid)) {
        case Success(Some(admin)) => complete(admin)
        case Success(None) =>
          complete(HttpResponse(StatusCodes.NotFound))
        case Failure(e) =>
          logger.error(s"get admin by id $adminid failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
      }
    }
  }

  @Path("/{adminid}")
  @ApiOperation(value = "update admin", nickname = "update-admin", consumes = "application/x-www-form-urlencoded", httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "adminId", required = true, dataType = "integer", paramType = "path", defaultValue = ""),
    new ApiImplicitParam(name = "password", required = true, dataType = "string", paramType = "form", defaultValue = ""),
    new ApiImplicitParam(name = "adminName", required = true, dataType = "string", paramType = "form", defaultValue = ""),
    new ApiImplicitParam(name = "email", required = true, dataType = "string", paramType = "form", defaultValue = ""),
    new ApiImplicitParam(name = "wxid", required = false, dataType = "string", paramType = "form", defaultValue = "")
  ))
  def putXRoute = path("api" / "v1" / "admins" / IntNumber) { adminid =>
    put {
      formFields(('password, 'adminName, 'email, 'wxid.?)) { (password, adminName, email, wxid) =>
        onComplete(AdminDAO.update(adminid, password, adminName, email, wxid)) {
          case Success(r) => complete(HttpResponse(StatusCodes.OK))
          case Failure(e) =>
            logger.error(s"update admin by id $adminid failed: $e")
            complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
        }
      }
    }
  }

  @Path("/{adminid}")
  @ApiOperation(value = "delete admin", nickname = "delete-admin", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "adminId", required = true, dataType = "integer", paramType = "path")
  ))
  def deleteXRoute = path("api" / "v1" / "admins" / IntNumber) { adminid =>
    delete {
      onComplete(AdminDAO.delete(adminid)) {
        case Success(_) => complete(HttpResponse(StatusCodes.NoContent))
        case Failure(e) =>
          logger.error(s"delete admin by id $adminid failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
      }
    }
  }

  @Path("/{adminid}/groups")
  @ApiOperation(value = "get groups for admin", nickname = "get-groups-for-admin", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "adminId", required = true, dataType = "integer", paramType = "path")
  ))
  def getAdminGroupsRoute = path("api" / "v1" / "admins" / IntNumber / "groups") { adminid =>
    import com.heqiying.fundmng.gate.model.GroupJsonSupport._

    import scala.concurrent.ExecutionContext.Implicits.global
    get {
      val groupIds = GroupDAO.getGroupsForAdmin(adminid)
      val groups = groupIds.flatMap { xs =>
        Future.sequence(xs.map { groupid =>
          GroupDAO.getOne(groupid)
        })
      }.map(_.flatten)

      onComplete(groups) {
        case Success(r) => complete(r)
        case Failure(e) =>
          logger.error(s"get groups for admin $adminid failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
      }
    }
  }
}
