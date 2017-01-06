import java.sql.{ DriverManager, SQLException }

object PostgresqlConnectTest extends App {

  println("-------- PostgreSQL "
    + "JDBC Connection Testing ------------");

  try {

    Class.forName("org.postgresql.Driver");

  } catch {
    case e: ClassNotFoundException => {

      println("Where is your PostgreSQL JDBC Driver? Include in your library path!")
      e.printStackTrace()
    }
  }

  println("PostgreSQL JDBC Driver Registered!")

  try {

    val connection = DriverManager.getConnection(
      "jdbc:postgresql://localhost:5432/activiti", "fuyf", "fuyf"
    )

    if (connection != null) {
      println("You made it, take control your database now!")
    } else {
      println("Failed to make connection!")
    }

  } catch {
    case e: SQLException => {
      println("Connection Failed! Check output console")
      e.printStackTrace()
    }
  }
}
