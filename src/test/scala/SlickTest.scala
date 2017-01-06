import com.heqiying.fundmng.gate.model._

import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._

object SlickTest extends App {
  val sqls =
    DBSchema.admins.schema.createStatements ++
    DBSchema.groups.schema.createStatements ++
    DBSchema.authorities.schema.createStatements ++
    DBSchema.authorityGroupMapping.schema.createStatements ++
    DBSchema.groupAdminMappings.schema.createStatements

  println(sqls.mkString(";\n"))
}
