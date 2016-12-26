import java.io.File

object JavaFileTest extends App {
//  val path = "/home/fuyf/hosts"
  val path = "/etc/hosts"
  val file = new File(path)
  println(file.getName())
  println(file.getParent())
  println(file.length())
  println(file.hashCode())
  println(file.lastModified())
}
