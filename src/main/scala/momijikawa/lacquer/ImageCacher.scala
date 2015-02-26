package momijikawa.lacquer

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.caching.{Cache, LruCache}
import spray.can.Http
import spray.http.HttpResponse
import spray.routing.RequestContext

import scala.concurrent.Future
import scala.concurrent.duration._
import scalaz.Scalaz._
import scalaz._

class ImageCacher(implicit val system: ActorSystem) {
  implicit val executionContext = system.dispatcher
  implicit val timeout: Timeout = 1 minutes

  val cacheEverything: Cache[HttpResponse] = LruCache()

  private val noCacheFlag = Seq("private", "no-cache", "no-store", "must-revalidate")

  private def cacheable(resp: HttpResponse): Boolean = {
    val isCacheProhibited = for (
      cacheControl ← resp.headers.find(header ⇒ header.name == "Cache-Control")
    ) yield noCacheFlag.exists(value ⇒ cacheControl.value.contains(value))

    val hasProhibitPragma = for (
      pragma ← resp.headers.find(header ⇒ header.name == "Pragma")
    ) yield pragma.value.contains("no-cache")

    !(isCacheProhibited | false || (hasProhibitPragma | false))
  }

  private def isImage(resp: HttpResponse): Boolean = {
    val responseHasImage = for (
      contentType ← resp.headers.find(header ⇒ header.name == "Content-Type")
    ) yield contentType.value.contains("image/")
    responseHasImage | false
  }

  def useCache(ctx: RequestContext): Future[HttpResponse] = {
    cacheEverything.get(ctx.request.message) match {
      case Some(cache: Future[HttpResponse]) ⇒
        println("found cache")
        cache

      case None ⇒
        println("no cache")
        val response: Future[HttpResponse] = (IO(Http) ? ctx.request).mapTo[HttpResponse]

        response.onSuccess {
          case resp if cacheable(resp) && isImage(resp) ⇒
            cacheEverything(ctx.request.message, () ⇒ Future(resp))
        }

        response
    }
  }
}
