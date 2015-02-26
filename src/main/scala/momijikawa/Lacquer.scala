package momijikawa

import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import momijikawa.lacquer.ImageCacher
import spray.http.HttpResponse
import spray.httpx.marshalling.MetaToResponseMarshallers
import spray.httpx.marshalling.MetaToResponseMarshallers.futureMarshaller
import spray.routing.{HttpServiceActor, RequestContext, Route}

import scala.concurrent.Future
import scala.concurrent.duration._
import scalaz.Scalaz._
import scalaz._

class Lacquer extends HttpServiceActor with ActorLogging {
  self: Actor ⇒
  implicit val actorSystem = this.context.system
  implicit val executionContext = actorSystem.dispatcher
  implicit val timeout: Timeout = 1 minutes

  val cache = new ImageCacher

  def route: Route = (ctx: RequestContext) ⇒ {
    println(ctx.request.uri)
    ctx.request.headers.find(_.name == "Host") >>= {
      (header: spray.http.HttpHeader) ⇒
        println("Request host: " + header.value).some
    }
    println(
      s"""Protocol: ${ctx.request.protocol.value} <${ctx.request.method.value}>
         | -> ${ctx.request.uri.authority.host.address}:${ctx.request.uri.authority.port}""".stripMargin)
    ctx.complete(fetch(ctx))
  }

  protected def fetch(ctx: RequestContext): Future[HttpResponse] = {
    cache.useCache(ctx)
  }

  def receive: Receive = runRoute(route)
}
