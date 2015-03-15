package momijikawa.lacquer

import akka.actor.{ Actor, ActorLogging }
import akka.util.Timeout
import spray.http.HttpResponse
import spray.httpx.marshalling.MetaToResponseMarshallers.futureMarshaller
import spray.routing.{ HttpServiceActor, RequestContext, Route }

import scala.concurrent.Future
import scala.concurrent.duration._

class Lacquer extends HttpServiceActor with ActorLogging {
  self: Actor ⇒
  implicit val actorSystem = this.context.system
  implicit val executionContext = actorSystem.dispatcher
  implicit val timeout: Timeout = 1 minutes

  val cache = new ImageCacher

  def receive: Receive = runRoute(route)

  def route: Route = (ctx: RequestContext) ⇒ {
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
    cache.processCtx(ctx)
  }
}
