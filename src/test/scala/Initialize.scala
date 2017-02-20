import java.io.{File, PrintWriter}

import akka.actor.ActorSystem
import akka.http.scaladsl.util.FastFuture
import akka.stream.ActorMaterializer
import com.heqiying.fundmng.gate.dao.ActiIdentityRPC
import com.heqiying.fundmng.gate.database.MainDBProfile._
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import com.heqiying.fundmng.gate.model._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io.Source

object GenInitTableSqls extends App {
  val schemaSqls =
    DBSchema.admins.schema.createStatements ++
      DBSchema.groups.schema.createStatements ++
      DBSchema.authorities.schema.createStatements ++
      DBSchema.authorityGroupMapping.schema.createStatements ++
      DBSchema.groupAdminMappings.schema.createStatements ++
      DBSchema.accessRecords.schema.createStatements ++
      DBSchema.uploads.schema.createStatements

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
    DBSchema.accessRecords.schema.create,
    DBSchema.uploads.schema.create
  )

  Await.result(schemaSqls.foldLeft(FastFuture.successful[Any](()))((fs, x) => fs.flatMap(_ => db.run(x))), Duration.Inf)
}

object InitData extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  val now = Some(System.currentTimeMillis())
  val initData = Seq(
    // init admins
    DBSchema.admins += Admin("fuyf", "fuyf", "YiFeng", "fuyifeng@heqiying.com", None, now, now),
    DBSchema.admins += Admin("zy", "zy", "ZhaoYue", "zhaoyue@heqiying.com", None, now, now),
    DBSchema.admins += Admin("zh", "zh", "ZhaoHui", "yinzhaohui@heqiying.com", None, now, now),

    // init groups
    DBSchema.groups += Group(Groups.GroupNameInputer, Groups.GroupTypeAdmin),
    DBSchema.groups += Group(Groups.GroupNameReviewer, Groups.GroupTypeAdmin),
    DBSchema.groups += Group(Groups.GroupNameSystemAdmin, Groups.GroupTypeAdmin),

    // init authorities

    // init group admin mapping
    DBSchema.groupAdminMappings += GroupAdminMapping(None, Groups.GroupNameInputer, "zy"),
    DBSchema.groupAdminMappings += GroupAdminMapping(None, Groups.GroupNameInputer, "zh"),
    DBSchema.groupAdminMappings += GroupAdminMapping(None, Groups.GroupNameReviewer, "zy"),
    DBSchema.groupAdminMappings += GroupAdminMapping(None, Groups.GroupNameSystemAdmin, "fuyf")
  )

  Await.result(initData.foldLeft(FastFuture.successful[Any](()))((fs, x) => fs.flatMap(_ => db.run(x))), Duration.Inf)
}

object InitActiviti extends App {
  // initialize ActivitiUserGroups
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  val actiRPC = ActiIdentityRPC.create(None)

  for {
    _ <- actiRPC.deleteMemberFromGroup(Groups.GroupNameInputer, "zy")
    _ <- actiRPC.deleteMemberFromGroup(Groups.GroupNameInputer, "zh")
    _ <- actiRPC.deleteMemberFromGroup(Groups.GroupNameReviewer, "zy")
    _ <- actiRPC.deleteMemberFromGroup(Groups.GroupNameSystemAdmin, "fuyf")

    _ <- actiRPC.deleteGroup(Groups.GroupNameInputer)
    _ <- actiRPC.deleteGroup(Groups.GroupNameReviewer)
    _ <- actiRPC.deleteGroup(Groups.GroupNameSystemAdmin)
    _ <- actiRPC.deleteUser("fuyf")
    _ <- actiRPC.deleteUser("zy")
    _ <- actiRPC.deleteUser("zh")

    _ <- actiRPC.createUser("fuyf", "YiFeng", "fuyifeng@heqiying.com")
    _ <- actiRPC.createUser("zy", "ZhaoYue", "zhaoyue@heqiying.com")
    _ <- actiRPC.createUser("zh", "ZhaoHui", "zhaohui@heqiying.com")
    _ <- actiRPC.createGroup(Groups.GroupNameInputer, Groups.GroupNameInputer, Groups.GroupTypeAdmin)
    _ <- actiRPC.createGroup(Groups.GroupNameReviewer, Groups.GroupNameReviewer, Groups.GroupTypeAdmin)
    _ <- actiRPC.createGroup(Groups.GroupNameSystemAdmin, Groups.GroupNameSystemAdmin, Groups.GroupTypeAdmin)

    _ <- actiRPC.addMemberToGroup(Groups.GroupNameInputer, "zy")
    _ <- actiRPC.addMemberToGroup(Groups.GroupNameInputer, "zh")
    _ <- actiRPC.addMemberToGroup(Groups.GroupNameReviewer, "zy")
    _ <- actiRPC.addMemberToGroup(Groups.GroupNameSystemAdmin, "fuyf")
  } yield ()
}
