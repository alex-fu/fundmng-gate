package com.heqiying.fundmng.gate.dao

import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.database.MainDBProfile._
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api.stringColumnExtensionMethods
import com.heqiying.fundmng.gate.model.Admins
import com.heqiying.fundmng.gate.utils.{ QueryParam, SortRule }
import slick.ast.{ Node, Ordering }
import slick.lifted.{ AbstractTable, LiteralColumn, Rep, TableQuery }

import scala.reflect.runtime.universe._
import scala.reflect.ClassTag

abstract class ComplexQuery[T <: AbstractTable[_]: TypeTag: ClassTag] extends LazyLogging {
  def tableQ: TableQuery[T]

  def get(qp: QueryParam) = {
    def getStringColumns(row: T): Iterable[Rep[String]] = {
      val reps = typeOf[T].members.filter(_.isMethod).map(_.asMethod).filter(_.returnType == typeOf[Rep[String]])
      val mirror = runtimeMirror(row.getClass.getClassLoader).reflect(row)
      val r = reps.map { s =>
        mirror.reflectMethod(s)().asInstanceOf[Rep[String]]
      }
      r
    }

    def getSortColumns(row: T)(implicit t3: TypeTag[Rep[_]]): Iterable[(String, Rep[_])] = {
      val reps = typeOf[T].members.filter(_.isMethod).map(_.asMethod).filter(_.returnType.typeSymbol == typeOf[Rep[_]].typeSymbol)
        .filter(_.name != TermName("column"))
      val mirror = runtimeMirror(row.getClass.getClassLoader).reflect(row)
      val r = reps.map { s =>
        s.name.decodedName.toString -> mirror.reflectMethod(s)().asInstanceOf[Rep[_]]
      }
      r
    }

    def seq2Ordered(s: Iterable[(Rep[_], Boolean)]) = {
      val columns = s.foldRight(Vector.empty[(Node, Ordering)]) {
        case ((rep, asc), r) =>
          (rep.toNode, if (!asc) slick.ast.Ordering().desc else slick.ast.Ordering()) +: r
      }
      new slick.lifted.Ordered(columns)
    }

    val q0 = tableQ

    val q1 = qp.q match {
      case Some(queryString) if queryString.nonEmpty =>
        q0.filter { row =>
          val r0: Iterable[Rep[String]] = getStringColumns(row)
          r0.foldLeft(LiteralColumn(false).bind) { (s, x) =>
            s || (x like s"%$queryString%")
          }
        }
      case _ => q0
    }

    val q2 = qp.sort match {
      case Some(sortString) =>
        val sortRule = SortRule(sortString)
        q1.sortBy { row =>
          val r0: Iterable[Option[(Rep[_], Boolean)]] = sortRule.rule.map {
            case (column, ascending) =>
              getSortColumns(row).find(_._1 == column).map(x => (x._2, ascending))
          }
          seq2Ordered(r0.flatten)
        }
      case None => q1
    }
    val q3 = qp.size match {
      case Some(size) =>
        val dropped = for {
          page <- qp.page
          size <- qp.size
        } yield page * size
        q2.drop(dropped.getOrElse(0)).take(size)
      case _ => q2
    }
    val q = q3.result
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }
}
