package com.heqiying.fundmng.gate.api

import java.util.UUID
import javax.ws.rs.Path

import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.model.Uploads
import com.heqiying.fundmng.gate.service.GateApp
import com.heqiying.fundmng.gate.utils.{ QueryParam, QueryResultJsonSupport }
import io.swagger.annotations.{ Api, ApiImplicitParam, ApiImplicitParams, ApiOperation }

import scala.util.{ Failure, Success, Try }

@Api(value = "Upload file API", produces = "application/json", protocols = "http")
@Path("/")
class UploadAPI(implicit app: GateApp) extends LazyLogging {
  val routes = fileUploadRoute ~ getUploadsRoute

  @Path("/upload")
  @ApiOperation(value = "upload file", nickname = "upload-file", httpMethod = "POST", consumes = "multipart/form-data")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "file", required = true, dataType = "file", paramType = "form")
  ))
  def fileUploadRoute = path("upload") {
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
    import spray.json.DefaultJsonProtocol._
    post {
      formFields(("file.name", "file.content_type", "file.path", "file.md5", "file.size")) { (name, contentType, path, md5, size) =>
        val uuid = UUID.randomUUID().toString
        app.addUploads(Uploads(uuid, name, Some(contentType), path, md5, Try(size.toLong).getOrElse(0)))
        complete(Map("uuid" -> uuid, "name" -> name))
      }
    }
  }

  @Path("/api/v1/uploads")
  @ApiOperation(value = "get uploads", nickname = "get-uploads-files", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "sort", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "page", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "size", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "q", required = false, dataType = "string", paramType = "query")
  ))
  def getUploadsRoute = path("api" / "v1" / "uploads") {
    get {
      import com.heqiying.fundmng.gate.model.UploadsJsonSupport._
      parameters('sort.?, 'page.as[Int].?, 'size.as[Int].?, 'q.?).as(QueryParam) { qp =>
        onComplete(app.getUploads(qp)) {
          case Success(r) =>
            implicit val m = QueryResultJsonSupport.queryResultJsonFormat[Uploads]()
            complete(r)
          case Failure(e) =>
            logger.error(s"get uploads failed! Errors: $e")
            complete(HttpResponse(StatusCodes.InternalServerError))
        }
      }
    }
  }

}
