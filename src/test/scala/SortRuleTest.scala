import com.heqiying.fundmng.gate.utils.SortRule

object SortRuleTest extends App {
  val str1 = "loginName,asc"
  val r1 = SortRule(str1)
  println(r1.rule)

  val str2 = "loginName,desc"
  val r2 = SortRule(str2)
  println(r2.rule)

  val str3 = "loginName,asc;createdAt,desc"
  val r3 = SortRule(str3)
  println(r3.rule)

  val str4 = "loginName,desc;createdAt,asc"
  val r4 = SortRule(str4)
  println(r4.rule)
}
