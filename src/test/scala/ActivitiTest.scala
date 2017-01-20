import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import com.heqiying.fundmng.gate.interface.ActivitiInterface
import com.heqiying.fundmng.gate.model.activiti._
import spray.json._

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait ActivitiTestTool {
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher

  val activitiInterface = new ActivitiInterface("kermit", Some("kermit"))
  //  val activitiInterface = new ActivitiInterface("fozzie", Some("fozzie"))
  val timeout = 10.seconds

  def debugResponse(relativeUri: String, resp: Future[HttpResponse]) = {
    resp onComplete {
      case Success(r) =>
        println(s"Response from $relativeUri(${r.status}):")
        r.entity.toStrict(timeout).map { strict =>
          println(s"${strict.data.utf8String.parseJson.prettyPrint}")
        }
      case Failure(e) =>
        println(s"access to $relativeUri failed: $e")
    }
  }

  def get(relativeUri: String) = {
    debugResponse(relativeUri, activitiInterface.get(relativeUri))
  }

  def post(relativeUri: String, headers: Seq[HttpHeader] = Nil, entity: RequestEntity = HttpEntity.Empty) = {
    debugResponse(relativeUri, activitiInterface.post(relativeUri, headers, entity))
  }

  def put(relativeUri: String, headers: Seq[HttpHeader] = Nil, entity: RequestEntity = HttpEntity.Empty) = {
    debugResponse(relativeUri, activitiInterface.put(relativeUri, headers, entity))
  }

  def delete(relativeUri: String) = {
    debugResponse(relativeUri, activitiInterface.delete(relativeUri))
  }

  def deploy(relativeUri: String, filepath: String) = {
    val p = Paths.get(filepath)
    Try(p.toRealPath()) match {
      case Success(rp) =>
        val form: FormData = Multipart.FormData.fromPath("deploy", ContentTypes.`application/octet-stream`, rp)
        val entity = Marshal(form).to[RequestEntity]
        val response = entity.flatMap { e =>
          activitiInterface.post(relativeUri, Nil, e)
        }

        response.onComplete {
          case Success(r) =>
            r.entity.toStrict(timeout).map { strict =>
              println(s"Response from $relativeUri(${r.status}):\n${strict.data.utf8String.parseJson.prettyPrint}")
            }
          case Failure(e) =>
            println(s"deploy on $relativeUri failed: $e")
        }
      case Failure(e) =>
        println(s"Can't find file: $filepath")
    }
  }
}

object ActivitiGetsTest extends App with ActivitiTestTool {
  //  get("/repository/deployments")
  //  get("/repository/process-definitions")
  //  get("/repository/models")
  //  get("/runtime/tasks")
  //  get("/management/jobs")
    get("/identity/users")
//  get("/identity/groups")
}

object ActivitiNewDeploymentTest extends App with ActivitiTestTool {
  deploy("/repository/deployments", "/tmp/review.bpmn")
}

object ActivitiAddUserTest extends App with ActivitiTestTool {

  import UserJsonSupport._

  val user = User("zh", "zh", "", "zhaohui@heqiying.com", Some("Heqiying5252"), None)
  val entity = Marshal(user).to[RequestEntity]
  entity.map { e =>
    post("/identity/users", Nil, e)
  }
}

object ActivitiUpdateUserTest extends App with ActivitiTestTool {
  import UserJsonSupport._

  val user = User("zh", "zh", "", "zhaohui@heqiying.com", Some("Heqiying5252"), None)
  val entity = Marshal(user).to[RequestEntity]
  entity.map { e =>
    put("/identity/users/" + user.id, Nil, e)
  }
}

object ActivitiDeleteUserTest extends App with ActivitiTestTool {
  delete("/identity/users/ccc")
}