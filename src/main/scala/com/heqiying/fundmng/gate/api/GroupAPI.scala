package com.heqiying.fundmng.gate.api

import javax.ws.rs.Path

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.dao.{ ActiIdentityRPC, AdminDAO, AuthorityDAO, GroupDAO }
import com.heqiying.fundmng.gate.directives.AuthDirective._
import com.heqiying.fundmng.gate.model.{ Group, Groups }
import io.swagger.annotations.{ Api, ApiImplicitParam, ApiImplicitParams, ApiOperation }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{ Failure, Success }

@Api(value = "Group API", produces = "application/json")
@Path("/api/v1/groups")
class GroupAPI(implicit val system: ActorSystem, val mat: ActorMaterializer) extends LazyLogging {
  val routes = getRoute ~ postRoute ~ getXRoute ~ deleteXRoute ~
    getGroupAdminsRoute ~ putGroupAdminsRoute ~
    getInvestorGroupAuthoritiesRoute ~ putInvestorGroupAuthoritiesRoute ~
    getGroupAuthoritiesRoute ~ putGroupAuthoritiesRoute

  @ApiOperation(value = "get groups", nickname = "get-groups", httpMethod = "GET")
  def getRoute = path("api" / "v1" / "groups") {
    import com.heqiying.fundmng.gate.model.GroupJsonSupport._
    get {
      onComplete(GroupDAO.getAll) {
        case Success(r) => complete(r)
        case Failure(e) =>
          logger.error(s"get groups failed: $e")
          complete(HttpResponse(StatusCodes.InternalServerError))
      }
    }
  }

  @ApiOperation(value = "create a new group", nickname = "create-group", consumes = "application/x-www-form-urlencoded", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "groupName", required = true, dataType = "string", paramType = "form", defaultValue = "")
  ))
  def postRoute = path("api" / "v1" / "groups") {
    import com.heqiying.fundmng.gate.model.GroupJsonSupport._
    post {
      formFields('groupName) { groupName =>
        extractAccesser { accesser =>
          val group = Group(groupName, Groups.GroupTypeAdmin)
          logger.info(s"add new group: $group")
          val actiRPC = ActiIdentityRPC.create(accesser)
          val f = for {
            r1 <- actiRPC.createGroup(groupName, groupName, group.groupType)
            if Seq(StatusCodes.Created, StatusCodes.Conflict) contains r1.status
            _ <- GroupDAO.insert(group)
          } yield ()
          onComplete(f) {
            case Success(r) => complete(group)
            case Failure(e) =>
              logger.error(s"post groups failed: $e")
              complete(HttpResponse(StatusCodes.InternalServerError))
          }
        }
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
      onComplete(GroupDAO.getOne(groupName)) {
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
        logger.info(s"delete group: $groupName")
        val actiRPC = ActiIdentityRPC.create(accesser)
        val f = for {
          r1 <- actiRPC.deleteGroup(groupName)
          if Seq(StatusCodes.NoContent, StatusCodes.NotFound) contains r1.status
          _ <- GroupDAO.delete(groupName)
        } yield ()
        onComplete(f) {
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

    import scala.concurrent.ExecutionContext.Implicits.global
    get {
      val adminNames = GroupDAO.getAdminsInGroup(groupName)
      val admins = adminNames.flatMap { xs =>
        Future.sequence(xs.map { adminName =>
          AdminDAO.getOne(adminName)
        })
      }.map(_.flatten)

      onComplete(admins) {
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
        logger.info(s"update admins in group $groupName with $adminNames")
        extractAccesser { accesser =>
          val actiRPC = ActiIdentityRPC.create(accesser)
          val f = for {
            existedAdminNames <- GroupDAO.getAdminsInGroup(groupName)
            s0 = existedAdminNames.toSet
            s1 = adminNames.toSet
            toDeletes = s0 -- s1
            toAdds = s1 -- s0
            r1 <- Future.sequence(toDeletes.map(ys => actiRPC.deleteMemberFromGroup(groupName, ys)))
            if r1.forall(r => Seq(StatusCodes.NoContent, StatusCodes.NotFound).contains(r.status))
            r2 <- Future.sequence(toAdds.map(ys => actiRPC.addMemberToGroup(groupName, ys)))
            if r2.forall(r => Seq(StatusCodes.Created, StatusCodes.Conflict).contains(r.status))
            _ <- GroupDAO.postAdminsInGroup(groupName, adminNames)
          } yield ()
          onComplete(f) {
            case Success(r) => complete(HttpResponse(StatusCodes.OK))
            case Failure(e) =>
              logger.error(s"post admins to group $groupName failed: $e")
              complete(HttpResponse(StatusCodes.InternalServerError))
          }
        }
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

    import scala.concurrent.ExecutionContext.Implicits.global
    get {
      val authorityNames: Future[Seq[String]] = AuthorityDAO.getAuthoritiesInGroup(groupName)
      val authorities = authorityNames.flatMap { xs =>
        Future.sequence(xs.map { authName =>
          AuthorityDAO.getOne(authName)
        })
      }.map(_.flatten)

      onComplete(authorities) {
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
        logger.info(s"update authorities on group $groupName with $authorityNames")
        onComplete(AuthorityDAO.postAuthoritiesInGroup(groupName, authorityNames)) {
          case Success(r) => complete(HttpResponse(StatusCodes.OK))
          case Failure(e) =>
            logger.error(s"post authorities on group $groupName failed: $e")
            complete(HttpResponse(StatusCodes.InternalServerError))
        }
      }
    }
  }

  @Path("/investorgroup/authorities")
  @ApiOperation(value = "get authorities on investorgroup", nickname = "get-authorities-on-investorgroup", httpMethod = "GET")
  def getInvestorGroupAuthoritiesRoute = path("api" / "v1" / "groups" / "investorgroup" / "authorities") {
    import com.heqiying.fundmng.gate.model.AuthorityJsonSupport._

    import scala.concurrent.ExecutionContext.Implicits.global
    get {
      val authorityNames: Future[Seq[String]] =
        for {
          investorGroup <- GroupDAO.getOrCreateInvestorGroup()
          if investorGroup.nonEmpty
          authorityNames <- AuthorityDAO.getAuthoritiesInGroup(investorGroup.get.groupName)
        } yield authorityNames
      val authorities = authorityNames.flatMap { xs =>
        Future.sequence(xs.map { authName =>
          AuthorityDAO.getOne(authName)
        })
      }.map(_.flatten)

      onComplete(authorities) {
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

    import scala.concurrent.ExecutionContext.Implicits.global
    put {
      entity(as[Seq[String]]) { authorityNames =>
        logger.info(s"update authorities on investor group with $authorityNames")
        val f = for {
          investorGroup <- GroupDAO.getOrCreateInvestorGroup()
          if investorGroup.nonEmpty
          r <- AuthorityDAO.postAuthoritiesInGroup(investorGroup.get.groupName, authorityNames)
        } yield r
        onComplete(f) {
          case Success(r) => complete(HttpResponse(StatusCodes.OK))
          case Failure(e) =>
            logger.error(s"post authorities on investor group failed: $e")
            complete(HttpResponse(StatusCodes.InternalServerError))
        }
      }
    }
  }
}
