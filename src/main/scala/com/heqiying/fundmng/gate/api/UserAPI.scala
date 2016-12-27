package com.heqiying.fundmng.gate.api

import javax.ws.rs.Path

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.model.User
import com.heqiying.fundmng.gate.directives.MultipartFormDirective._
import io.swagger.annotations.{ Api, ApiImplicitParam, ApiImplicitParams, ApiOperation }
import java.io.File

import scala.util.{ Failure, Success }

@Api(value = "User API", produces = "application/json")
@Path("/api/v1/user")
class UserAPI(implicit system: ActorSystem) extends LazyLogging {

  val routes = getRoute ~ getXRoute ~ postRoute ~ getUserXIdentityRoute ~ postUserXIdentityRoute ~ getUserFileRoute

  @ApiOperation(value = "get users", nickname = "get-users", httpMethod = "GET")
  def getRoute = path("api" / "v1" / "user") {
    import com.heqiying.fundmng.gate.model.UserJsonSupport._
    get {
      val user1 = User("fuyf", "fuyf")
      val user2 = User("hqy", "hqy")
      complete(Seq(user1, user2))
    }
  }

  @Path("/{userid}")
  @ApiOperation(value = "get user by id", nickname = "get-user-by-id", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "userid", required = true, dataType = "integer", paramType = "path")
  ))
  def getXRoute = path("api" / "v1" / "user" / IntNumber) { userid =>
    import com.heqiying.fundmng.gate.model.UserJsonSupport._
    get {
      val user = User("fuyf", "fuyf")
      complete(user)
    }
  }

  @ApiOperation(value = "create or update user", nickname = "create-or-update-user", consumes = "multipart/form-data", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "username", required = true, dataType = "string", paramType = "form", defaultValue = ""),
    new ApiImplicitParam(name = "password", required = true, dataType = "string", paramType = "form", defaultValue = "")
  ))
  def postRoute = path("api" / "v1" / "user") {
    import com.heqiying.fundmng.gate.model.UserJsonSupport._
    post {
      val user = User("pass", "pass")
      complete(user)
    }
  }

  @Path("/{userid}/identity")
  @ApiOperation(value = "get user identities", nickname = "get-user-identities", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "userid", required = true, dataType = "integer", paramType = "path")
  ))
  def getUserXIdentityRoute = path("api" / "v1" / "user" / IntNumber / "identity") { userid =>
    import com.heqiying.fundmng.gate.directives.MultipartFormDirective.FileInfoJsonSupport._
    get {
      logger.info(s"get userid $userid's identities")
      val path1 = "/home/fuyf/github/mychart/idea.png"
      val file1 = new File(path1)
      val fileInfo1 = FileInfo(file1.getName, path1, file1.length, Map("hash1" -> "", "hash2" -> ""), "")

      val path2 = "/etc/hosts"
      val file2 = new File(path2)
      val fileInfo2 = FileInfo(file2.getName, path2, file2.length, Map("hash1" -> "", "hash2" -> ""), "")

      complete(Seq(fileInfo1, fileInfo2))
    }
  }

  @Path("/{userid}/identity")
  @ApiOperation(value = "post user identity", nickname = "post-user-identity", consumes = "multipart/form-data", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "userid", required = true, dataType = "integer", paramType = "path"),
    new ApiImplicitParam(name = "identityid", required = true, dataType = "string", paramType = "form"),
    new ApiImplicitParam(name = "identity", required = true, dataType = "file", paramType = "form")
  ))
  def postUserXIdentityRoute = path("api" / "v1" / "user" / IntNumber / "identity") { userid =>
    import com.heqiying.fundmng.gate.directives.MultipartFormDirective.FileInfoJsonSupport._
    post {
      extractExecutionContext { implicit ec =>
        collectFormData { datamapFuture =>
          onComplete {
            logger.info("Received multipart form data:")
            datamapFuture.map { datamap =>
              datamap.foreach { tuple =>
                logger.info(s"$tuple")
              }
            }
            datamapFuture
          } {
            case Success(r) => complete(r)
            case Failure(e) =>
              logger.error("", e)
              complete((500, "internal server error"))
          }
        }
      }
    }
  }

  @Path("/file")
  @ApiOperation(value = "get user file", nickname = "get-user-file", produces = "application/octet-stream", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "hash1", required = true, dataType = "string", paramType = "form"),
    new ApiImplicitParam(name = "hash2", required = true, dataType = "string", paramType = "form")
  ))
  def getUserFileRoute = path("api" / "v1" / "user" / "file") {
    get {
      formFields('hash1, 'hash2) { (hash1, hash2) =>
        logger.info(s"get user file with hash1 = $hash1 and hash2 = $hash2")
        //        val path = "/home/fuyf/github/mychart/idea.png"
        val path = "/etc/hosts"
        downloadFile(path)
      }
    }
  }
}
