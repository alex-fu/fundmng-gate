import java.io.{File, PrintWriter}

import akka.http.scaladsl.util.FastFuture
import com.heqiying.fundmng.gate.database.MainDBProfile._
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import com.heqiying.fundmng.gate.model._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io.Source

object SlickTest extends App {
  val schemaSqls =
    DBSchema.admins.schema.createStatements ++
      DBSchema.groups.schema.createStatements ++
      DBSchema.authorities.schema.createStatements ++
      DBSchema.authorityGroupMapping.schema.createStatements ++
      DBSchema.groupAdminMappings.schema.createStatements

  //  println(schemaSqls.mkString(";\n"))

  val outputFile = "/home/fuyf/project/fundmng-flyway/sql/V1__CreateAdminTables.sql"
  val writer = new PrintWriter(new File(outputFile))
  writer.write(schemaSqls.mkString(";\n"))
  writer.close()

  Source.fromFile(outputFile).foreach(print)
}

object InitData extends App {
  val initData = Seq(
    // init admins
    DBSchema.admins += Admin(None, "fuyf", "fuyf", "Yifeng", "fuyifeng@heqiying.com", None, System.currentTimeMillis(), None),
    DBSchema.admins += Admin(None, "zy", "zy", "Zhaoyue", "zhaoyue@heqiying.com", None, System.currentTimeMillis(), None),
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