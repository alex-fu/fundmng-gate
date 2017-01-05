import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
import com.heqiying.fundmng.gate.model.Users

object SlickTest extends App {
  val users: TableQuery[Users] = TableQuery[Users]

  users.schema.create.statements.foreach(println)
}
