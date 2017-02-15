object RandomFileName extends App {
  def to36Char(x: Int): Option[Char] = {
    if (0 <= x && x < 26) Some(('a' + x).toChar)
    else if (26 <= x && x < 36) Some(('0' + x - 26).toChar)
    else None
  }

  def retry(f: => Option[Char]): Char = {
    val r = f
    if (r.isEmpty) retry(f) else r.get
  }

  def genRandomString(len: Int) = {
    val r = new util.Random
    def genRandomChar: Char = {
      retry(to36Char(r.nextInt.abs % 36))
    }
    (1 to len).map(_ => genRandomChar).mkString
  }

  println(genRandomString(10))
}
