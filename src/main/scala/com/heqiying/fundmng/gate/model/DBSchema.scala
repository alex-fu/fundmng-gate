package com.heqiying.fundmng.gate.model

import slick.lifted.AbstractTable

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

object DBSchema {
  import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._

  //  class SortAssistant[T <: AbstractTable[_]: TypeTag: ClassTag](q: TableQuery[T]) {
  //    def getSortColumns: Iterable[(String, Any)] = {
  //      //      val reps = typeOf[T].members.filter(_.isMethod).map(_.asMethod).filter(_.returnType.typeSymbol == typeOf[Rep[_]].typeSymbol)
  //      //        .filter(_.name != TermName("column"))
  //      val reps = typeOf[T].members.filter(_.isMethod).map(_.asMethod).filter(_.returnType == typeOf[slick.lifted.Rep[String]])
  //      val row = q.baseTableRow
  //      println(s"b: ${row.hashCode()}")
  //      val mirror = runtimeMirror(row.getClass.getClassLoader).reflect(row)
  //      val r = reps.map { s =>
  //        val a = mirror.reflectMethod(s)()
  //        println(s"a: ${a.hashCode()}")
  //        s.name.decodedName.toString -> a
  //      } //.asInstanceOf[slick.lifted.Rep[String]].toNode)
  //      r
  //    }
  //  }

  val admins = TableQuery[Admins]
  //  val adminsSortColumns = new SortAssistant(admins).getSortColumns

  val groups = TableQuery[Groups]
  val authorities = TableQuery[Authorities]
  val groupAdminMappings = TableQuery[GroupAdminMappings]
  val authorityGroupMapping = TableQuery[AuthorityGroupMappings]
  val accessRecords = TableQuery[AccessRecords]
}
