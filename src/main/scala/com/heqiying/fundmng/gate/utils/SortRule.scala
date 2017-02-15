package com.heqiying.fundmng.gate.utils

class SortRule(val rule: Seq[(String, Boolean)]) // Seq[(columnName, ascending)]

object SortRule {
  def isAscending(s: Array[String]) = if (s.length == 2 && s(1).trim.toLowerCase == "desc") false else true
  def apply(sortString: String): SortRule = {
    val dilim1 = ","
    val dilim2 = ";"
    val rule = sortString.split(dilim2).map(x => x.split(dilim1)).filter(s => s.nonEmpty && s.length <= 2).map(s => (s(0).trim, isAscending(s))).toSeq
    new SortRule(rule)
  }
}