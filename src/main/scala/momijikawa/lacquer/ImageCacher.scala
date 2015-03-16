package momijikawa.lacquer

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.caching.{ Cache, LruCache }
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

  private def nor(seq: Seq[Boolean]) = !seq.reduce(_ || _)
  private def isAMemberOf(seq: Seq[_])(x: Any) = seq.contains(x)

  private def getHeaderValue(name: String, from: HttpResponse) =
    from.headers.find(_.name == name) ∘ (header ⇒ header.value)

  private def cacheable(resp: HttpResponse): Boolean = {
    val isCacheProhibited = getHeaderValue("Cache-Control", from = resp) ∘ isAMemberOf(noCacheFlag) getOrElse false
    val hasProhibitPragma = getHeaderValue("Pragma", from = resp) ∘
      { headerValueString ⇒ headerValueString.contains("no-cache") } getOrElse false

    nor(isCacheProhibited :: hasProhibitPragma :: Nil)
  }

  private def isImage(resp: HttpResponse): Boolean = {
    val hasImageMime = getHeaderValue("Content-Type", from = resp) ∘
      { headerValueString ⇒ headerValueString.contains("image/") }

    hasImageMime getOrElse false
  }

  def processCtx(ctx: RequestContext): Future[HttpResponse] = {
    import scala.util.{ Success, Failure }

    cacheEverything.get(ctx.request.message) match {
      case Some(cache: Future[HttpResponse]) ⇒
        println("found cache")
        cache

      case None ⇒
        println("no cache")
        val response: Future[HttpResponse] = (IO(Http) ? ctx.request).mapTo[HttpResponse]

        response.onComplete {
          case Success(resp) ⇒
          case resp if cacheable(resp.get) && isImage(resp.get) ⇒
            cacheEverything(ctx.request.message, () ⇒ Future(resp.get))
          case Failure(err) ⇒
            system.log.error(err.toString)
        }

        response
    }
  }
}
