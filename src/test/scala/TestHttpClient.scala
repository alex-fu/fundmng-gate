import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.Uri.{ Path, Query }
import akka.http.scaladsl.model.headers.{ Authorization, BasicHttpCredentials }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ FileIO, Sink, Source }

import scala.concurrent.{ ExecutionContext, Future }

object TestHttpClient {
  class Client(system: ActorSystem, host: String, port: Int, username: String, password: String) {
    private implicit val actorSystem = system
    private implicit val materializer = ActorMaterializer()
    private implicit val ec = system.dispatcher

    val server = Uri(s"http://$host:$port")
    val httpClient = Http(system).outgoingConnection(server.authority.host.address(), server.authority.port)
    import scala.collection.immutable.Seq
    val httpHeaders = Seq(
      Authorization(BasicHttpCredentials(username, password))
    )

    def uploadFile(relativeUri: String, file: File): Future[HttpResponse] = {
      val target = server.withPath(Path(relativeUri))

      val request = entity(file).map { entity =>
        HttpRequest(HttpMethods.POST, uri = target, headers = httpHeaders, entity = entity)
      }

      val response = Source.fromFuture(request).via(httpClient).runWith(Sink.head)
      response
    }

    def downloadFile(relativeUri: String, saveAs: File): Future[Long] = {
      val downloadUri = server.withPath(Path(relativeUri))
      //download file to local
      val response = Source.single(HttpRequest(uri = downloadUri, headers = httpHeaders)).via(httpClient).runWith(Sink.head)
      val downloaded = response.flatMap { response =>
        response.entity.dataBytes.runWith(FileIO.toPath(saveAs.toPath))
      }
      downloaded.map(_.getCount)
    }

    private def entity(file: File)(implicit ec: ExecutionContext): Future[RequestEntity] = {
      val entity = HttpEntity(MediaTypes.`application/octet-stream`, file.length(), FileIO.fromPath(file.toPath, chunkSize = 100000))
      val body = Source.single(
        Multipart.FormData.BodyPart(
          "uploadfile",
          entity,
          Map("filename" -> file.getName)
        )
      )
      val form = Multipart.FormData(body)

      Marshal(form).to[RequestEntity]
    }
  }
}
