package momijikawa

import akka.actor.{ ActorLogging, Actor }
import akka.util.Timeout
import momijikawa.lacquer.KanColleMessage.KanColleMessage
import momijikawa.lacquer.{ KanColle, ImageCacher }
import spray.http.HttpResponse
import spray.httpx.marshalling.MetaToResponseMarshallers
import spray.routing.{ HttpServiceActor, Route, RequestContext }
import scala.concurrent.duration._
import MetaToResponseMarshallers.futureMarshaller
import scala.concurrent.Future
import scalaz._
import Scalaz._

class Lacquer extends HttpServiceActor with ActorLogging {
  self: Actor ⇒
  implicit val actorSystem = this.context.system
  implicit val executionContext = actorSystem.dispatcher
  implicit val timeout: Timeout = 5 seconds

  val cache = new ImageCacher

  def route: Route = (ctx: RequestContext) ⇒ {
    println(ctx.request.uri)
    val fetchedResponse = fetch(ctx)
    ctx.complete(fetchedResponse)
  }

  protected def fetch(ctx: RequestContext): Future[HttpResponse] = {
    cache.useCache(ctx)
  }

  def receive: Receive = runRoute(route)
}
