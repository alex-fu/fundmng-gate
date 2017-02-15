package com.heqiying.fundmng.gate.api

import javax.ws.rs.Path

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.directives.AuthDirective._
import com.heqiying.fundmng.gate.model.{ Group, Groups }
import com.heqiying.fundmng.gate.service.GateApp
import com.heqiying.fundmng.gate.utils.{ QueryParam, QueryResultJsonSupport }
import io.swagger.annotations.{ Api, ApiImplicitParam, ApiImplicitParams, ApiOperation }

import scala.util.{ Failure, Success }

@Api(value = "Group API", consumes = "application/json", produces = "application/json")
@Path("/api/v1/groups")
class GroupAPI(implicit val app: GateApp, val system: ActorSystem, val mat: ActorMaterializer) extends LazyLogging {
  // Note: getInvestorGroupAuthoritiesRoute and putInvestorGroupAuthoritiesRoute MUST be
  // concat before getGroupAuthoritiesRoute and putGroupAuthoritiesRoute
  val routes = getRoute ~ postRoute ~ getXRoute ~ deleteXRoute ~
    getGroupAdminsRoute ~ putGroupAdminsRoute ~
    getInvestorGroupAuthoritiesRoute ~ putInvestorGroupAuthoritiesRoute ~
    getGroupAuthoritiesRoute ~ putGroupAuthoritiesRoute

  @ApiOperation(value = "get groups", nickname = "get-groups", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "sort", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "page", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "size", required = false, dataType = "string", paramType = "query"),
    new ApiImplicitParam(name = "q", required = false, dataType = "string", paramType = "query")
  ))
  def getRoute = path("api" / "v1" / "groups") {
    import com.heqiying.fundmng.gate.model.GroupJsonSupport._
    get {
      parameters('sort.?, 'page.as[Int].?, 'size.as[Int].?, 'q.?).as(QueryParam) { qp =>
        onComplete(app.getGroups(qp)) {
          case Success(r) =>
            implicit val m = QueryResultJsonSupport.queryResultJsonFormat[Group]()
            complete(r)
          case Failure(e) =>
            logger.error(s"get groups failed: $e")
            complete(HttpResponse(StatusCodes.InternalServerError))
        }
      }
    }
  }

  @ApiOperation(value = "create a new group", nickname = "create-group", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "groupName", required = true, dataType = "string", paramType = "body", defaultValue = "")
  ))
  def postRoute = path("api" / "v1" / "groups") {
    import com.heqiying.fundmng.gate.model.GroupJsonSupport._
    post {
      entity(as[String]) { groupName =>
        extractAccesser { accesser =>
          val group = Group(groupName, Groups.GroupTypeAdmin)
          onComplete(app.addGroup(group, accesser)) {
            case Success(r) => complete(group)
            case Failure(e) =>
              logger.error(s"post groups failed: $e")
              complete(HttpResponse(StatusCodes.InternalServerError))
          }
        }
      } ~ {
        logger.error(s"post groups failed! Errors: wrong argument")
        complete(HttpResponse(StatusCodes.BadRequest))
      }
    }
  }

  @Path("/{groupName}")
  @ApiOperation(value = "get group by name", nickname = "get-group-by-name", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "groupName", required = true, dataType = "string", paramType = "path")
  ))
  def getXRoute = path("api" / "v1" / "groups" / Segment) { groupName =>
    import com.heqiying.fundmng.gate.model.GroupJsonSupport._
    get {
      onComplete(app.getGroupByName(groupName)) {
        case Success(Some(r)) => complete(r)
        case Success(None) => complete(HttpResponse(StatusCodes.NotFound))
        case Failure(e) =>
          logger.error(s"get group $groupName failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError))
      }
    }
  }

  @Path("/{groupName}")
  @ApiOperation(value = "delete group", nickname = "delete-group", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "groupName", required = true, dataType = "string", paramType = "path")
  ))
  def deleteXRoute = path("api" / "v1" / "groups" / Segment) { groupName =>
    delete {
      extractAccesser { accesser =>
        onComplete(app.deleteGroup(groupName, accesser)) {
          case Success(_) => complete(HttpResponse(StatusCodes.NoContent))
          case Failure(e) =>
            logger.error(s"delete group $groupName failed: $e")
            complete(HttpResponse(StatusCodes.InternalServerError))
        }
      }
    }
  }

  @Path("/{groupName}/admins")
  @ApiOperation(value = "get admins in group", nickname = "get-admins-in-group", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "groupName", required = true, dataType = "string", paramType = "path")
  ))
  def getGroupAdminsRoute = path("api" / "v1" / "groups" / Segment / "admins") { groupName =>
    import com.heqiying.fundmng.gate.model.AdminJsonSupport._
    get {
      onComplete(app.getAdminsInGroup(groupName)) {
        case Success(r) => complete(r)
        case Failure(e) =>
          logger.error(s"get admins in group $groupName failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError))
      }
    }
  }

  @Path("/{groupName}/admins")
  @ApiOperation(value = "set admins in group", nickname = "set-admins-in-group", consumes = "application/json", httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "groupName", required = true, dataType = "integer", paramType = "path"),
    new ApiImplicitParam(name = "data", required = true, dataType = "string", paramType = "body", value = """["zy", "zh"]""")
  ))
  def putGroupAdminsRoute = path("api" / "v1" / "groups" / Segment / "admins") { groupName =>
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
    import spray.json.DefaultJsonProtocol._
    put {
      entity(as[Seq[String]]) { adminNames =>
        extractAccesser { accesser =>
          onComplete(app.updateAdminsInGroup(groupName, adminNames, accesser)) {
            case Success(r) => complete(HttpResponse(StatusCodes.OK))
            case Failure(e) =>
              logger.error(s"post admins to group $groupName failed: $e")
              complete(HttpResponse(StatusCodes.InternalServerError))
          }
        }
      } ~ {
        logger.error(s"update admins in group $groupName failed! Errors: wrong argument")
        complete(HttpResponse(StatusCodes.BadRequest))
      }
    }
  }

  @Path("/{groupName}/authorities")
  @ApiOperation(value = "get authorities on group", nickname = "get-authorities-on-group", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "groupName", required = true, dataType = "string", paramType = "path")
  ))
  def getGroupAuthoritiesRoute = path("api" / "v1" / "groups" / Segment / "authorities") { groupName =>
    import com.heqiying.fundmng.gate.model.AuthorityJsonSupport._
    get {
      onComplete(app.getAuthoritiesOnGroup(groupName)) {
        case Success(r) => complete(r)
        case Failure(e) =>
          logger.error(s"get authorities on group $groupName failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError))
      }
    }
  }

  @Path("/{groupName}/authorities")
  @ApiOperation(value = "set authorities on group", nickname = "set-authorities-on-group", consumes = "application/json", httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "groupName", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "data", required = true, dataType = "string", paramType = "body", value = """["AdminManage", "FundManage"]""")
  ))
  def putGroupAuthoritiesRoute = path("api" / "v1" / "groups" / Segment / "authorities") { groupName =>
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
    import spray.json.DefaultJsonProtocol._
    put {
      entity(as[Seq[String]]) { authorityNames =>
        onComplete(app.updateAuthoritiesOnGroup(groupName, authorityNames)) {
          case Success(r) => complete(HttpResponse(StatusCodes.OK))
          case Failure(e) =>
            logger.error(s"update authorities on group $groupName failed: $e")
            complete(HttpResponse(StatusCodes.InternalServerError))
        }
      }
    } ~ {
      logger.error(s"update authorities on group $groupName failed! Errors: wrong argument")
      complete(HttpResponse(StatusCodes.BadRequest))
    }
  }

  @Path("/investorgroup/authorities")
  @ApiOperation(value = "get authorities on investorgroup", nickname = "get-authorities-on-investorgroup", httpMethod = "GET")
  def getInvestorGroupAuthoritiesRoute = path("api" / "v1" / "groups" / "investorgroup" / "authorities") {
    import com.heqiying.fundmng.gate.model.AuthorityJsonSupport._
    get {
      onComplete(app.getAuthoritiesOnInvestorGroup()) {
        case Success(r) => complete(r)
        case Failure(e) =>
          logger.error(s"get authorities on investor group failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError))
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
    put {
      entity(as[Seq[String]]) { authorityNames =>
        onComplete(app.updateAuthoritiesOnInvestorGroup(authorityNames)) {
          case Success(r) => complete(HttpResponse(StatusCodes.OK))
          case Failure(e) =>
            logger.error(s"update authorities on investor group failed: $e")
            complete(HttpResponse(StatusCodes.InternalServerError))
        }
      } ~ {
        logger.error(s"update authorities on investor group failed! Errors: wrong argument")
        complete(HttpResponse(StatusCodes.BadRequest))
      }
    }
  }
}
