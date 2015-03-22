package momijikawa.lacquer

import akka.actor.ActorSystem
import akka.util.Timeout
import spray.caching.{ Cache, LruCache }
import spray.http._
import spray.routing.RequestContext
import spray.client.pipelining._
import spray.httpx.encoding.Gzip

import scala.concurrent.Future
import scala.concurrent.duration._

class ImageCacher(implicit val system: ActorSystem) {
  implicit val executionContext = system.dispatcher
  implicit val timeout: Timeout = 1 minutes

  val cacheEverything: Cache[HttpResponse] = LruCache()

  private val noCacheFlag = Seq("private", "no-cache", "no-store", "must-revalidate")

  private def nor(seq: Seq[Boolean]) = !seq.reduce(_ || _)
  private def isAMemberOf(seq: Seq[_])(x: Any) = seq.contains(x)

  private def getHeaderValue(name: String, from: HttpResponse) =
    from.headers.find(_.name == name) map (header ⇒ header.value)

  private def cacheable(resp: HttpResponse): Boolean = {
    val isCacheProhibited = getHeaderValue("Cache-Control", from = resp) exists isAMemberOf(noCacheFlag)
    val hasProhibitPragma = getHeaderValue("Pragma", from = resp) exists { headerValueString ⇒ headerValueString.contains("no-cache") }

    nor(isCacheProhibited :: hasProhibitPragma :: Nil)
  }

  private def isImage(resp: HttpResponse): Boolean = {
    val hasImageMime = getHeaderValue("Content-Type", from = resp) map
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
        val pipeline: HttpRequest => Future[HttpResponse] = sendReceive ~> decode(Gzip)
        val response: Future[HttpResponse] = pipeline(ctx.request)

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
