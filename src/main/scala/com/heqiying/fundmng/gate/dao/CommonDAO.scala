package com.heqiying.fundmng.gate.dao

import com.heqiying.fundmng.gate.common.LazyLogging
import com.heqiying.fundmng.gate.database.MainDBProfile._
import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import com.heqiying.fundmng.gate.utils.{ QueryParam, QueryResult, SortRule }
import slick.ast.{ BaseTypedType, Node, Ordering }
import slick.lifted.{ LiteralColumn, Rep, TableQuery }
import slick.profile.RelationalProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

abstract class CommonDAO[E, T <: RelationalProfile#Table[E]: TypeTag: ClassTag] extends LazyLogging {
  def tableQ: TableQuery[T]
  def pk: String

  def insert(elem: E) = {
    val q = tableQ += elem
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  def update[PKType: BaseTypedType](v: PKType, elem: E) = {
    val q = tableQ.filter(row => getPKColumn(row)(pk).asInstanceOf[Rep[PKType]] === v).update(elem)
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  def delete[PKType: BaseTypedType](v: PKType) = {
    val q = tableQ.filter(row => getPKColumn(row)(pk).asInstanceOf[Rep[PKType]] === v).delete
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  def getAll = {
    val q = tableQ.result
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q)
  }

  def getOne[PKType: BaseTypedType](v: PKType) = {
    val q = tableQ.filter(row => getPKColumn(row)(pk).asInstanceOf[Rep[PKType]] === v).result
    sqlDebug(q.statements.mkString(";\n"))
    db.run(q.headOption)
  }

  def get(qp: QueryParam): Future[QueryResult[E]] = {
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
              getSortColumns(row).find(_._1 == column.toLowerCase).map(x => (x._2, ascending))
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

    val currentElements = db.run(q)
    val totalElements = db.run(q1.length.result)

    for {
      xs <- currentElements
      t <- totalElements
    } yield {
      val totalPages = qp.size.map { s =>
        if (t % s != 0) t / s + 1 else t / s
      }.getOrElse(1)
      val page = qp.page.getOrElse(0)
      new QueryResult[E](
        xs,
        t,
        totalPages,
        xs.length,
        page,
        qp.size.getOrElse(t),
        page == 0,
        page >= totalPages - 1
      )
    }
  }

  private def getPKColumn(row: T)(pk: String): Rep[_] = {
    val reps = typeOf[T].members.filter(_.isMethod).map(_.asMethod).filter(_.name == TermName(pk))
    val mirror = runtimeMirror(row.getClass.getClassLoader).reflect(row)
    val r = reps.map { s =>
      mirror.reflectMethod(s)().asInstanceOf[Rep[_]]
    }.head
    r
  }

  private def getStringColumns(row: T): Iterable[Rep[String]] = {
    val reps = typeOf[T].members.filter(_.isMethod).map(_.asMethod).filter(_.returnType == typeOf[Rep[String]])
    val mirror = runtimeMirror(row.getClass.getClassLoader).reflect(row)
    val r = reps.map { s =>
      mirror.reflectMethod(s)().asInstanceOf[Rep[String]]
    }
    r
  }

  private def getSortColumns(row: T)(implicit t3: TypeTag[Rep[_]]): Iterable[(String, Rep[_])] = {
    val reps = typeOf[T].members.filter(_.isMethod).map(_.asMethod).filter(_.returnType.typeSymbol == typeOf[Rep[_]].typeSymbol)
      .filter(_.name != TermName("column"))
    val mirror = runtimeMirror(row.getClass.getClassLoader).reflect(row)
    val r = reps.map { s =>
      s.name.decodedName.toString.toLowerCase -> mirror.reflectMethod(s)().asInstanceOf[Rep[_]]
    }
    r
  }
}
