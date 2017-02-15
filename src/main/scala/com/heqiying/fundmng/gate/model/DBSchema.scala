package com.heqiying.fundmng.gate.model

object DBSchema {

  import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._

  val admins = TableQuery[Admins]
  val groups = TableQuery[Groups]
  val authorities = TableQuery[Authorities]
  val groupAdminMappings = TableQuery[GroupAdminMappings]
  val authorityGroupMapping = TableQuery[AuthorityGroupMappings]
  val accessRecords = TableQuery[AccessRecords]
}
