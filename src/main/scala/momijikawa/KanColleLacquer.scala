package momijikawa

import akka.actor.ActorRef
import BasicLacquerConfiguration._
import KanColleLacquerUtil._
import momijikawa.lacquer.KanColle
import momijikawa.lacquer.KanColleMessage.KanColleMessage
import spray.http.{ HttpHeaders, HttpHeader, Uri, HttpResponse }
import spray.routing.RequestContext
import scalaz._
import Scalaz._

class KanColleLacquer(wsServer: ActorRef) extends Lacquer {
  val spoofElectricEyeURL: PartialFunction[RequestContext, RequestContext] =
    spoofURL(kanColleElectricEyeHookHostFrom)(kanColleElectricEyeHookHostTo)

  // リクエストの何も書き換えないダミー関数
  // 関係無いリクエストは無視させる
  val spoofNothing: PartialFunction[RequestContext, RequestContext] = {
    case anything: RequestContext ⇒ anything
  }

  override def route = (ctx: RequestContext) ⇒ {
    val analyze = KanColle.kanColleAnalyze(ctx.request)
    // 特定のコンテキストを書き換える
    val spoofedCtx = ctx |> (spoofElectricEyeURL orElse spoofNothing)
    super.route(spoofedCtx)

    analyze match {
      case Some(converter: (HttpResponse ⇒ KanColleMessage)) ⇒
        fetch(ctx).onSuccess {
          case response: HttpResponse ⇒
            val kanColleMessage = converter(response)
            log.info(kanColleMessage.toString.take(100))
            wsServer ! kanColleMessage
        }
      case None ⇒
    }
  }
}
