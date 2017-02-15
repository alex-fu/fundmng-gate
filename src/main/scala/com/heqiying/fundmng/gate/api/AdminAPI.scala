package com.heqiying.fundmng.gate.api

import javax.ws.rs.Path

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.directives.AuthDirective._
import com.heqiying.fundmng.gate.model.{ Admin, UpdateAdminRequest }
import com.heqiying.fundmng.gate.service.GateApp
import com.heqiying.fundmng.gate.utils.{ QueryParam, QueryResultJsonSupport }
import io.swagger.annotations.{ Api, ApiImplicitParam, ApiImplicitParams, ApiOperation }

import scala.util.{ Failure, Success }

@Api(value = "Admin API", consumes = "application/json", produces = "application/json")
@Path("/api/v1/admins")
class AdminAPI(implicit val app: GateApp, val system: ActorSystem, val mat: ActorMaterializer) extends LazyLogging {
  val routes = getRoute ~ postRoute ~ getXRoute ~ putXRoute ~ deleteXRoute ~ getAdminGroupsRoute

  @ApiOperation(value = "get admins", nickname = "get-admins", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "sort", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "page", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "size", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "q", required = false, dataType = "string", paramType = "query")
  ))
  def getRoute = path("api" / "v1" / "admins") {
    import com.heqiying.fundmng.gate.model.AdminJsonSupport._
    get {
      parameters('sort.?, 'page.as[Int].?, 'size.as[Int].?, 'q.?).as(QueryParam) { qp =>
        onComplete(app.getAdmins(qp)) {
          case Success(r) =>
            implicit val m = QueryResultJsonSupport.queryResultJsonFormat[Admin]()
            complete(r)
          case Failure(e) =>
            logger.error(s"get admins failed! Errors: $e")
            complete(HttpResponse(StatusCodes.InternalServerError))
        }
      }
    }
  }

  @ApiOperation(value = "create a new admin", nickname = "create-admin", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "request", value = """{"loginName":"test", "password":"test", "adminName":"test", "email":"test@test", "wxid":"test"}""", required = true, dataType = "string", paramType = "body", defaultValue = "")
  ))
  def postRoute = path("api" / "v1" / "admins") {
    import com.heqiying.fundmng.gate.model.AdminJsonSupport._
    post {
      entity(as[Admin]) { admin =>
        extractAccesser { accesser =>
          val now = System.currentTimeMillis()
          onComplete(app.addAdmin(admin.copy(createdAt = Some(now), updatedAt = Some(now)), accesser)) {
            case Success(Right(x)) => complete(x)
            case Success(Left(e)) => complete(HttpResponse(StatusCodes.BadRequest, entity = e))
            case Failure(e) =>
              logger.error(s"update admins failed! Errors: $e")
              complete(HttpResponse(StatusCodes.InternalServerError))
          }
        }
      } ~ {
        logger.error(s"update admins failed! Errors: wrong argument")
        complete(HttpResponse(StatusCodes.BadRequest))
      }
    }
  }

  @Path("/{loginName}")
  @ApiOperation(value = "get admin by name", nickname = "get-admin-by-name", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "loginName", required = true, dataType = "string", paramType = "path")
  ))
  def getXRoute = path("api" / "v1" / "admins" / Segment) { loginName =>
    import com.heqiying.fundmng.gate.model.AdminJsonSupport._
    get {
      onComplete(app.getAdminByName(loginName)) {
        case Success(Some(admin)) => complete(admin.copy(password = ""))
        case Success(None) =>
          complete(HttpResponse(StatusCodes.NotFound))
        case Failure(e) =>
          logger.error(s"get admin $loginName failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError))
      }
    }
  }

  @Path("/{loginName}")
  @ApiOperation(value = "update admin", nickname = "update-admin", httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "loginName", required = true, dataType = "string", paramType = "path", defaultValue = ""),
    new ApiImplicitParam(name = "request", value = """{"password":"test", "adminName":"test", "email":"test@test", "wxid":"test"}""", required = true, dataType = "string", paramType = "body", defaultValue = "")
  ))
  def putXRoute = path("api" / "v1" / "admins" / Segment) { loginName =>
    import com.heqiying.fundmng.gate.model.UpdateAdminRequestJsonSupport._
    put {
      entity(as[UpdateAdminRequest]) { req =>
        extractAccesser { accesser =>
          onComplete(app.updateAdmin(loginName, req, accesser)) {
            case Success(r) => complete(HttpResponse(StatusCodes.OK))
            case Failure(e) =>
              logger.error(s"update admin $loginName failed: $e")
              complete(HttpResponse(StatusCodes.InternalServerError))
          }
        }
      } ~ {
        logger.error(s"update admin $loginName failed! Errors: wrong argument")
        complete(HttpResponse(StatusCodes.BadRequest))
      }
    }
  }

  @Path("/{loginName}")
  @ApiOperation(value = "delete admin", nickname = "delete-admin", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "loginName", required = true, dataType = "string", paramType = "path")
  ))
  def deleteXRoute = path("api" / "v1" / "admins" / Segment) { loginName =>
    delete {
      extractAccesser { accesser =>
        onComplete(app.deleteAdmin(loginName, accesser)) {
          case Success(_) => complete(HttpResponse(StatusCodes.NoContent))
          case Failure(e) =>
            logger.error(s"delete admin $loginName failed: $e")
            complete(HttpResponse(StatusCodes.InternalServerError))
        }
      }
    }
  }

  @Path("/{loginName}/groups")
  @ApiOperation(value = "get groups for admin", nickname = "get-groups-for-admin", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "loginName", required = true, dataType = "string", paramType = "path")
  ))
  def getAdminGroupsRoute = path("api" / "v1" / "admins" / Segment / "groups") { loginName =>
    import com.heqiying.fundmng.gate.model.GroupJsonSupport._
    get {
      onComplete(app.getAdminGroups(loginName)) {
        case Success(r) => complete(r)
        case Failure(e) =>
          logger.error(s"get groups for admin $loginName failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError))
      }
    }
  }
}
