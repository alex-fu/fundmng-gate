import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ Authorization, BasicHttpCredentials }
import akka.stream.ActorMaterializer

import scala.concurrent.Future
import scala.concurrent.duration._

object ActivitiTest extends App {
  import TestHttpClient._

  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val mat = ActorMaterializer()
  implicit val timeout = 1.seconds

  val client = new Client(system, "127.0.0.1", 8080, "kermit", "kermit")

  def issueRequest(method: HttpMethod, uri: String, entity: RequestEntity): Future[HttpResponse] = {
    import scala.collection.immutable.Seq
    val headers = Seq(
      Authorization(BasicHttpCredentials("kermit", "kermit"))
    )
    val req = HttpRequest(method = method, uri = "http://localhost:8080/activiti-rest" + uri, headers = headers, entity = entity)
    Http().singleRequest(req)
  }

  def issueGetRequest(uri: String): Future[HttpResponse] = {
    issueRequest(HttpMethods.GET, uri, HttpEntity.Empty)
  }

  def issuePostRequest(uri: String, entity: RequestEntity): Future[HttpResponse] = {
    issueRequest(HttpMethods.POST, uri, entity)
  }

  val path = "/tmp/review.bpmn"
  val file = new File(path)
  client.uploadFile("/activiti-rest/service/repository/deployments", file).onSuccess {
    case r => r.entity.toStrict(timeout).map { strict =>
      println(s"upload deployment file $path, response code = ${r.status}, response body = ${strict.data.utf8String}")
    }
  }

  issueGetRequest("/service/repository/deployments").onSuccess {
    case r => r.entity.toStrict(timeout).map { strict =>
      println(s"deployments: ${strict.data.utf8String}")
    }
  }

  issueGetRequest("/service/repository/process-definitions").onSuccess {
    case r => r.entity.toStrict(timeout).map { strict =>
      println(s"process definitions: ${strict.data.utf8String}")
    }
  }

}
