package com.heqiying.fundmng.gate.directives

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.PathMatcher.{ Matched, Unmatched }
import akka.http.scaladsl.server._

import scala.annotation.tailrec

object ExtendPathMatchers {

  object NonExtractSegment extends PathMatcher0 {
    def apply(path: Path) = path match {
      case Path.Segment(segment, tail) ⇒ Matched(tail, ())
      case _ ⇒ Unmatched
    }
  }

  def separateOnSlashes(string: String): PathMatcher0 = {
    @tailrec def split(ix: Int = 0, matcher: PathMatcher0 = null): PathMatcher0 = {
      val nextIx = string.indexOf('/', ix)
      def append(m: PathMatcher0) = if (matcher eq null) m else matcher / m
      val placeholder = """\{(.*)\}""".r.unanchored
      val placeString = if (nextIx < 0) string.substring(ix) else string.substring(ix, nextIx)
      if (nextIx < 0) {
        placeString match {
          case "" => matcher
          case placeholder(_) =>
            append(NonExtractSegment)
          case _ =>
            append(placeString)
        }
      } else {
        placeString match {
          case "" => split(nextIx + 1, matcher)
          case placeholder(_) =>
            split(nextIx + 1, append(NonExtractSegment))
          case _ =>
            split(nextIx + 1, append(placeString))
        }
      }
    }
    split()
  }
}
