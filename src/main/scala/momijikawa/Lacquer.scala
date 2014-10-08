package momijikawa

import akka.actor.Actor
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.caching.{ Cache, LruCache }
import spray.can.Http
import spray.httpx.marshalling.{ MetaToResponseMarshallers, ToResponseMarshaller }
import spray.routing.{ HttpServiceActor, Route, RequestContext }
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import spray.http.HttpResponse
import spray.caching
import MetaToResponseMarshallers.futureMarshaller
import scalaz._
import Scalaz._

class Lacquer extends HttpServiceActor {
  self: Actor ⇒
  implicit val actorSystem = this.context.system
  implicit val timeout: Timeout = 5 seconds

  val cacheEverything: Cache[HttpResponse] = LruCache()

  val route: Route = (ctx: RequestContext) ⇒ {
    println(ctx.request.uri)
    cacheEverything.get(ctx.request.message) match {
      case Some(cache: Future[HttpResponse]) ⇒
        println("found cache")
        ctx.complete(cache)

      case None ⇒
        println("no cache")
        val response: Future[HttpResponse] = (IO(Http) ? ctx.request).mapTo[HttpResponse]
        val noCacheFlag = Seq("private", "no-cache", "no-store", "must-revalidate")

        def cacheable(resp: HttpResponse): Boolean = {
          val isCacheProhibited = for (
            cacheControl ← resp.headers.find(header ⇒ header.name == "Cache-Control")
          ) yield noCacheFlag.exists(value ⇒ cacheControl.value.contains(value))

          val hasProhibitPragma = for (
            pragma ← resp.headers.find(header ⇒ header.name == "Pragma")
          ) yield pragma.value.contains("no-cache")

          !(isCacheProhibited | false || (hasProhibitPragma | false))
        }

        def isImage(resp: HttpResponse): Boolean = {
          val responseHasImage = for (
            contentType ← resp.headers.find(header ⇒ header.name == "Content-Type")
          ) yield contentType.value.contains("image/")
          responseHasImage | false
        }

        response.onSuccess {
          case resp if cacheable(resp) && isImage(resp) ⇒
            cacheEverything(ctx.request.message, () ⇒ Future(resp))
        }

        ctx.complete(response)
      case others ⇒
        println(s"unknown: $others")
    }
  }

  def receive: Receive = runRoute(route)
}
