import java.io.{File, PrintWriter}

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.util.FastFuture
import com.heqiying.fundmng.gate.common.AppConfig
import com.heqiying.fundmng.gate.database.MainDBProfile._
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import com.heqiying.fundmng.gate.model._
import com.heqiying.fundmng.gate.model.activiti

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

object GenInitTableSqls extends App {
  import scala.concurrent.ExecutionContext.Implicits.global
  val schemaSqls =
    DBSchema.admins.schema.createStatements ++
      DBSchema.groups.schema.createStatements ++
      DBSchema.authorities.schema.createStatements ++
      DBSchema.authorityGroupMapping.schema.createStatements ++
      DBSchema.groupAdminMappings.schema.createStatements ++
      DBSchema.accessRecords.schema.createStatements

  //  println(schemaSqls.mkString(";\n"))

  val outputFile = "/home/fuyf/project/fundmng-flyway/sql/V1__CreateGateTables.sql"
  val writer = new PrintWriter(new File(outputFile))
  writer.write(schemaSqls.mkString(";\n"))
  writer.close()

  Source.fromFile(outputFile).foreach(print)
}

object InitTable extends App {
  import scala.concurrent.ExecutionContext.Implicits.global
  val schemaSqls = Seq(
    DBSchema.admins.schema.create,
    DBSchema.groups.schema.create,
    DBSchema.authorities.schema.create,
    DBSchema.authorityGroupMapping.schema.create,
    DBSchema.groupAdminMappings.schema.create,
    DBSchema.accessRecords.schema.create
  )

  Await.result(schemaSqls.foldLeft(FastFuture.successful[Any](()))((fs, x) => fs.flatMap(_ => db.run(x))), Duration.Inf)
}

object InitData extends App {
  import scala.concurrent.ExecutionContext.Implicits.global
  val initData = Seq(
    // init admins
    DBSchema.admins += Admin(None, "fuyf", "fuyf", "YiFeng", "fuyifeng@heqiying.com", None, System.currentTimeMillis(), None),
    DBSchema.admins += Admin(None, "zy", "zy", "ZhaoYue", "zhaoyue@heqiying.com", None, System.currentTimeMillis(), None),
    DBSchema.admins += Admin(None, "zh", "zh", "ZhaoHui", "yinzhaohui@heqiying.com", None, System.currentTimeMillis(), None),

    // init groups
    DBSchema.groups += Group(None, Groups.GroupNameInputer, Groups.GroupTypeAdmin),
    DBSchema.groups += Group(None, Groups.GroupNameReviewer, Groups.GroupTypeAdmin),
    DBSchema.groups += Group(None, Groups.GroupNameSystemAdmin, Groups.GroupTypeAdmin),

    // init authorities

    // init group admin mapping
    DBSchema.groupAdminMappings += GroupAdminMapping(None, 1, 2),
    DBSchema.groupAdminMappings += GroupAdminMapping(None, 1, 3),
    DBSchema.groupAdminMappings += GroupAdminMapping(None, 2, 2),
    DBSchema.groupAdminMappings += GroupAdminMapping(None, 3, 1)
  )

  Await.result(initData.foldLeft(FastFuture.successful[Any](()))((fs, x) => fs.flatMap(_ => db.run(x))), Duration.Inf)
}

object InitActivitiUserGroup extends App with ActivitiTestTool {
  import activiti.UserJsonSupport._

  def addUser(user: activiti.User) = {
    val entity = Marshal(user).to[RequestEntity]
    entity.map { e =>
      post("/identity/users", Nil, e)
    }
  }
//  addUser(activiti.User("zh", "ZhaoHui", "", "zhaohui@heqiying.com", Some(AppConfig.fundmngGate.activiti.defaultPassword), None))
  addUser(activiti.User("zy", "ZhaoYue", "", "zhaoyue@heqiying.com", Some(AppConfig.fundmngGate.activiti.defaultPassword), None))
//  addUser(activiti.User("fuyf", "YiFeng", "", "fuyifeng@heqiying.com", Some(AppConfig.fundmngGate.activiti.defaultPassword), None))
}