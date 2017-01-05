package db.migration

import java.sql.Connection

import com.heqiying.fundmng.gate.model.Users
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import slick.jdbc.JdbcBackend.{ BaseSession, DatabaseDef }
import slick.jdbc.{ JdbcBackend, JdbcDataSource }
import slick.lifted.TableQuery
import slick.util.AsyncExecutor

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{ Failure, Try }

class UnmanagedJdbcDataSource(conn: Connection) extends JdbcDataSource {
  def createConnection() = conn
  def close() = ()
}

class UnmanagedSession(database: DatabaseDef) extends BaseSession(database) {
  override def close() = ()
}

class UnmanagedDatabase(conn: Connection) extends JdbcBackend.DatabaseDef(new UnmanagedJdbcDataSource(conn), AsyncExecutor("UmanagedDatabase-AsyncExecutor", 1, -1)) {
  override def createSession() = new UnmanagedSession(this)
}

trait SlickMigration extends JdbcMigration {
  def slickMigrate(db: UnmanagedDatabase)

  override def migrate(connection: Connection) = {
    val db = new UnmanagedDatabase(connection)
    Try {
      slickMigrate(db)
    } match {
      case Failure(e) => println(s"Slick migration failed! Error: $e")
      case _ =>
    }
  }
}

class V1__001_CreateUserTable extends SlickMigration {
  override def slickMigrate(db: UnmanagedDatabase): Unit = {
    val users = TableQuery[Users]

    import com.heqiying.fundmng.gate.database.MainDBProfile.profile.api._
    Await.result(db.run(users.schema.create), 10.seconds)
  }
}