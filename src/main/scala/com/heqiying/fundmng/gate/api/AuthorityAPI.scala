package com.heqiying.fundmng.gate.api

import javax.ws.rs.Path

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.model.Authority
import com.heqiying.fundmng.gate.service.GateApp
import com.heqiying.fundmng.gate.utils.{ QueryParam, QueryResultJsonSupport }
import io.swagger.annotations.{ Api, ApiImplicitParam, ApiImplicitParams, ApiOperation }

import scala.util.{ Failure, Success }

@Path("/api/v1/authorities")
@Api(value = "Authority API", produces = "application/json")
class AuthorityAPI(implicit val app: GateApp, val system: ActorSystem, val mat: ActorMaterializer) extends LazyLogging {
  val routes = getRoute ~ putRoute

  @ApiOperation(value = "get authorities", nickname = "get-authorities", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "sort", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "page", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "size", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "q", required = false, dataType = "string", paramType = "query")
  ))
  def getRoute = path("api" / "v1" / "authorities") {
    import com.heqiying.fundmng.gate.model.AuthorityJsonSupport._
    get {
      parameters('sort.?, 'page.as[Int].?, 'size.as[Int].?, 'q.?).as(QueryParam) { qp =>
        onComplete(app.getAuthorities(qp)) {
          case Success(r) =>
            implicit val m = QueryResultJsonSupport.queryResultJsonFormat[Authority]()
            complete(r)
          case Failure(e) =>
            logger.error(s"get authorities failed: $e")
            complete(HttpResponse(StatusCodes.InternalServerError))
        }
      }
    }
  }

  @ApiOperation(value = "upsert authorities", nickname = "upsert-authorities", httpMethod = "PUT", consumes = "multipart/form-data")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "authorities_file", required = true, dataType = "file", paramType = "form")
  ))
  def putRoute = path("api" / "v1" / "authorities") {
    import com.heqiying.fundmng.gate.directives.MultipartFormDirective._

    put {
      extractExecutionContext { implicit ec =>
        collectFormData { datamapFuture =>
          val authorities_filepath = datamapFuture.map { datamap =>
            datamap.getOrElse("authorities_file", Left("")) match {
              case Right(fileInfo) =>
                logger.info(s"upsert authorities ${fileInfo.fileName}")
                Some(fileInfo.targetFile)
              case _ => None
            }
          }

          onComplete(app.updateAuthoritiesFromFile(authorities_filepath)) {
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
