package com.heqiying.fundmng.gate.api

import javax.ws.rs.Path

import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.dao.AuthorityDAO
import com.heqiying.fundmng.gate.model.Authority
import io.swagger.annotations.{ Api, ApiImplicitParam, ApiImplicitParams, ApiOperation }

import scala.concurrent.Future
import scala.io.Source
import scala.util.{ Failure, Success }

@Path("/api/v1/authorities")
@Api(value = "Authority API", produces = "application/json")
class AuthorityAPI extends LazyLogging {
  val routes = getRoute ~ putRoute

  @ApiOperation(value = "get authorities", nickname = "get-authorities", httpMethod = "GET")
  def getRoute = path("api" / "v1" / "authorities") {
    import com.heqiying.fundmng.gate.model.AuthorityJsonSupport._
    get {
      onComplete(AuthorityDAO.getAll) {
        case Success(r) => complete(r)
        case Failure(e) =>
          logger.error(s"get authorities failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError))
      }
    }
  }

  @ApiOperation(value = "upsert authorities", nickname = "upsert-authorities", httpMethod = "PUT", consumes = "multipart/form-data")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "authorities_file", required = true, dataType = "file", paramType = "form")
  ))
  def putRoute = path("api" / "v1" / "authorities") {
    import com.heqiying.fundmng.gate.directives.MultipartFormDirective._
    import spray.json._
    import com.heqiying.fundmng.gate.model.AuthorityJsonSupport._
    put {
      extractExecutionContext { implicit ec =>
        collectFormData { datamapFuture =>
          val authorities_filename = datamapFuture.map { datamap =>
            datamap.getOrElse("authorities_file", Left("")) match {
              case Right(fileInfo) =>
                logger.info(s"upsert authorities ${fileInfo.fileName}")
                Some(fileInfo.targetFile)
              case _ => None
            }
          }
          val authorities = authorities_filename.map {
            case Some(filename) => Some(Source.fromFile(filename).mkString.parseJson.convertTo[Seq[Authority]])
            case _ => None
          }
          onComplete(authorities.flatMap {
            case Some(x) => AuthorityDAO.upsert(x).map(x => Right(x))
            case _ => Future(Left("please specify authorities file!"))
          }) {
            case Success(Right(_)) => complete(HttpResponse(StatusCodes.OK))
            case Success(Left(x)) => complete(HttpResponse(StatusCodes.BadRequest, entity = x))
            case Failure(e) =>
              logger.error(s"upsert authorities failed: $e")
              complete(HttpResponse(StatusCodes.InternalServerError))
          }
        }
      }
    }
  }

  //  @ApiOperation(value = "upsert authorities", nickname = "upsert-authorities", httpMethod = "PUT")
  //  @ApiImplicitParams(Array(
  //    new ApiImplicitParam(name = "data", required = true, dataType = "string", paramType = "body",
  //      value = """[{"authorityName":"FundManage","authorityLabel":"基金产品管理","expressions":[{"httpMethods":["GET","POST"],"pathExpression":"/api/v1/funds"},{"httpMethods":["GET","PUT","DELETE"],"pathExpression":"/api/v1/funds/{fundid}"}]}]""")
  //  ))
  //  def putRoute = path("api" / "v1" / "authorities") {
  //    import com.heqiying.fundmng.gate.model.AuthorityJsonSupport._
  //    put {
  //      entity(as[Seq[Authority]]) { authorities =>
  //        onComplete(AuthorityDAO.upsert(authorities)) {
  //          case Success(r) => complete(HttpResponse(StatusCodes.OK))
  //          case Failure(e) =>
  //            logger.error(s"upsert authorities failed: $e")
  //            complete(HttpResponse(StatusCodes.InternalServerError))
  //        }
  //      }
  //    }
  //  }
}
