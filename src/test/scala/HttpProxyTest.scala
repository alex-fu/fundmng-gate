import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{RequestContext, Route}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

object HttpProxyTest extends App {

  implicit val system = ActorSystem("Proxy")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val proxy = Route { (context: RequestContext) =>
    val request = context.request
    request.headers.foreach(x => println(s"${x.name} => ${x.value}"))
    println("Opening connection to localhost:7001")
    val flow = Http(system).outgoingConnection("localhost", 7001)
    val handler = Source.single(context.request)
      .map(r => r.removeHeader("Timeout-Access").
        addHeader(RawHeader("X-AccesserID", "id")).
        addHeader(RawHeader("X-AccesserLoginName", "loginName")).
        addHeader(RawHeader("X-AccesserName", "name")).
        addHeader(RawHeader("X-AccesserEmail", "email")).
        addHeader(RawHeader("X-AccesserWxID", "wxid")).
        addHeader(RawHeader("X-AccesserType", "type"))
      )
      .via(flow)
      .runWith(Sink.head)
      .flatMap(context.complete(_))
    handler
  }

  val binding = Http(system).bindAndHandle(handler = proxy, interface = "localhost", port = 9000)
}