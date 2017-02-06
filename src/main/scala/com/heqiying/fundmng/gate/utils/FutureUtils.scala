package com.heqiying.fundmng.gate.utils

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object FutureUtils {
  def toFutureOption[A](x: Option[Future[A]]): Future[Option[A]] = {
    x match {
      case Some(f) => f.map(Some(_))
      case None => Future.successful(None)
    }
  }
}
