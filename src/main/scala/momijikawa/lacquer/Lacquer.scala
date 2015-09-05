package momijikawa.lacquer

import akka.actor.{ Actor, ActorLogging }
import akka.util.Timeout
import spray.client.pipelining._
import spray.http.{ HttpRequest, HttpResponse }
import spray.httpx.encoding.Gzip
import spray.httpx.marshalling.MetaToResponseMarshallers.futureMarshaller
import spray.routing.{ HttpServiceActor, RequestContext, Route }
import spray.routing.directives.CompressResponseMagnet
import scala.concurrent.future
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._

class Lacquer extends HttpServiceActor with ActorLogging {
  self: Actor ⇒
  implicit val actorSystem = this.context.system
  implicit val executionContext = actorSystem.dispatcher
  implicit val timeout: Timeout = 1 minutes

  val cache = new ImageCacher

  def receive: Receive = runRoute(route)

  def route: Route = compressResponse(CompressResponseMagnet.fromUnit()) {
    (ctx: RequestContext) ⇒
      printReqInfo(ctx)
      ctx.complete(fetch(ctx))
  }

  def printReqInfo(ctx: RequestContext) = {
    println(ctx.request.uri)
    println(
      s"""Protocol: ${ctx.request.protocol.value} <${ctx.request.method.value}>
       | -> ${ctx.request.uri.authority.host.address}:${ctx.request.uri.authority.port}""".stripMargin)
    ctx.request.headers.find(_.name == "Host") foreach {
      (header: spray.http.HttpHeader) ⇒
        println("Request host: " + header.value)
    }
  }

  protected def fetch(ctx: RequestContext): Future[HttpResponse] = {
    val pipeline: HttpRequest ⇒ Future[HttpResponse] = sendReceive ~> decode(Gzip)
    future(Await.result(pipeline(ctx.request), 1 minutes))
    //cache.processCtx(ctx)
  }
}
