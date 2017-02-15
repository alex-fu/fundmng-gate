import com.heqiying.fundmng.gate.model.{Admins, DBSchema}
import slick.lifted.Rep

object ReflectTest extends App {
  import scala.reflect.runtime.universe._
//  val r = typeOf[Admins].members.filter(_.isMethod).map(_.asMethod).filter(_.returnType.typeSymbol == typeOf[Rep[_]].typeSymbol)
//    .filter(_.name != TermName("column"))
  val r = typeOf[Admins].members.filter(_.isMethod).map(_.asMethod).filter(_.returnType == typeOf[Rep[String]])
  println(r.map(_.name))

  val q = DBSchema.admins.baseTableRow
  val x = runtimeMirror(q.getClass.getClassLoader).reflect(q)
  val r0 = r.map(s => s.name.decodedName.toString -> x.reflectMethod(s)().asInstanceOf[Rep[_]].toString())
  println(r0)

}
