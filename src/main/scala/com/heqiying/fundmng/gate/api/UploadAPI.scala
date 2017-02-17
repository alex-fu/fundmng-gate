package com.heqiying.fundmng.gate.api

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.service.GateApp
import io.swagger.annotations.{ Api, ApiImplicitParam, ApiImplicitParams, ApiOperation }

@Api(value = "Upload file API", produces = "application/json", protocols = "http")
@Path("/upload")
class UploadAPI(implicit app: GateApp) extends LazyLogging {
  val routes = fileUploadRoute

  @ApiOperation(value = "upload file", nickname = "upload-file", httpMethod = "POST", consumes = "multipart/form-data")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "file", required = true, dataType = "file", paramType = "form")
  ))
  def fileUploadRoute = path("upload") {
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
    import spray.json.DefaultJsonProtocol._
    post {
      formFields(("file.name", "file.content_type", "file.path", "file.md5", "file.size")) { (name, contentType, path, md5, size) =>
        logger.info(s"upload file: $name, $contentType, $path, $md5, $size")
        complete(Map("name" -> name))
      }
    }
  }

}
