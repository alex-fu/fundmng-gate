import java.io.{File, PrintWriter}

import com.heqiying.fundmng.gate.model._
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._

import scala.io.Source

object SlickTest extends App {
  val sqls =
    DBSchema.admins.schema.createStatements ++
    DBSchema.groups.schema.createStatements ++
    DBSchema.authorities.schema.createStatements ++
    DBSchema.authorityGroupMapping.schema.createStatements ++
    DBSchema.groupAdminMappings.schema.createStatements

//  println(sqls.mkString(";\n"))

  val outputFile = "/home/fuyf/project/fundmng-flyway/sql/V1__CreateAdminTables.sql"
  val writer = new PrintWriter(new File(outputFile))
  writer.write(sqls.mkString(";\n"))
  writer.close()

  Source.fromFile(outputFile).foreach(print)
}
