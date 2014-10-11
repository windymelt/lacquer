package momijikawa

import akka.actor.Actor
import akka.util.Timeout
import momijikawa.laquer.KanColleMessage.KanColleMessage
import momijikawa.laquer.{ KanColle, ImageCacher }
import spray.http.HttpResponse
import spray.httpx.marshalling.MetaToResponseMarshallers
import spray.routing.{ HttpServiceActor, Route, RequestContext }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import MetaToResponseMarshallers.futureMarshaller
import scalaz._
import Scalaz._

class Lacquer extends HttpServiceActor {
  self: Actor ⇒
  implicit val actorSystem = this.context.system
  implicit val timeout: Timeout = 5 seconds

  val cache = new ImageCacher

  val route: Route = (ctx: RequestContext) ⇒ {
    println(ctx.request.uri)
    val analyze = KanColle.kanColleAnalyze(ctx.request)
    val fetchedResponse = cache.useCache(ctx)
    analyze match {
      case Some(converter: (HttpResponse ⇒ KanColleMessage)) ⇒
        fetchedResponse.onSuccess {
          case response: HttpResponse ⇒ println(converter(response))
        }
      case None ⇒
    }
    ctx.complete(fetchedResponse)
  }

  def receive: Receive = runRoute(route)
}
