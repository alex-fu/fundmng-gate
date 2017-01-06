package com.heqiying.fundmng.gate.api

import javax.ws.rs.Path

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.dao.AdminDAO
import com.heqiying.fundmng.gate.model.Admin
import io.swagger.annotations.{Api, ApiImplicitParam, ApiImplicitParams, ApiOperation}

import scala.util.{Failure, Success}

@Api(value = "Admin API", produces = "application/json")
@Path("/api/v1/admins")
class AdminAPI(implicit system: ActorSystem) extends LazyLogging {
  val routes = getRoute ~ postRoute ~ getXRoute ~ putXRoute
  //     ~ deleteXRoute

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

  @ApiOperation(value = "create or update admin", nickname = "create-or-update-admin", consumes = "application/x-www-form-urlencoded", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "loginName", required = true, dataType = "string", paramType = "form", defaultValue = ""),
    new ApiImplicitParam(name = "password", required = true, dataType = "string", paramType = "form", defaultValue = ""),
    new ApiImplicitParam(name = "adminName", required = true, dataType = "string", paramType = "form", defaultValue = ""),
    new ApiImplicitParam(name = "email", required = true, dataType = "string", paramType = "form", defaultValue = "")
  ))
  def postRoute = path("api" / "v1" / "admins") {
    import com.heqiying.fundmng.gate.model.AdminJsonSupport._
    post {
      formFields('loginName, 'password, 'adminName, 'email) { (loginName, password, adminName, email) =>
        val admin = Admin(None, loginName, password, adminName, email, System.currentTimeMillis(), None)
        logger.info(admin.toString)
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
    new ApiImplicitParam(name = "adminid", required = true, dataType = "integer", paramType = "path")
  ))
  def getXRoute = path("api" / "v1" / "admins" / IntNumber) { adminid =>
    import com.heqiying.fundmng.gate.model.AdminJsonSupport._
    get {
      onComplete(AdminDAO.getOne(adminid)) {
        case Success(Some(admin)) => complete(admin)
        case Success(None) =>
          complete(HttpResponse(StatusCodes.NotFound))
        case Failure(e) =>
          logger.error(s"get admin by id failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError, entity = e.toString))
      }
    }
  }

    @Path("/{adminid}")
    @ApiOperation(value = "modify admin", nickname = "modify-admin", consumes = "application/x-www-form-urlencoded", httpMethod = "PUT")
    @ApiImplicitParams(Array(
      new ApiImplicitParam(name = "password", required = true, dataType = "string", paramType = "form", defaultValue = ""),
      new ApiImplicitParam(name = "adminName", required = true, dataType = "string", paramType = "form", defaultValue = ""),
      new ApiImplicitParam(name = "email", required = true, dataType = "string", paramType = "form", defaultValue = "")
    ))
    def putXRoute = path("api" / "v1" / "admins" / IntNumber) { adminid =>
      put {
        formFields('password, 'adminName, 'email) { (password, adminName, email) =>
          onComplete(AdminDAO.update(password, adminName, email)) {
            case Success(r) => complete(r)
            case Failure(e) =>
              logger.error("", e)
              complete((500, "internal server error"))
          }
        }
      }
    }

  //  @Path("/file")
  //  @ApiOperation(value = "get user file", nickname = "get-user-file", produces = "application/octet-stream", httpMethod = "GET")
  //  @ApiImplicitParams(Array(
  //    new ApiImplicitParam(name = "hash1", required = true, dataType = "string", paramType = "form"),
  //    new ApiImplicitParam(name = "hash2", required = true, dataType = "string", paramType = "form")
  //  ))
  //  def getUserFileRoute = path("api" / "v1" / "user" / "file") {
  //    get {
  //      formFields('hash1, 'hash2) { (hash1, hash2) =>
  //        logger.info(s"get user file with hash1 = $hash1 and hash2 = $hash2")
  //        //        val path = "/home/fuyf/github/mychart/idea.png"
  //        val path = "/etc/hosts"
  //        downloadFile(path)
  //      }
  //    }
  //  }
}
