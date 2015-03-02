package momijikawa

import akka.actor.ActorRef
import momijikawa.BasicLacquerConfiguration._
import momijikawa.KanColleLacquerUtil._
import momijikawa.lacquer.KanColle
import momijikawa.lacquer.KanColleMessage.KanColleMessage
import spray.http.HttpResponse
import spray.routing.RequestContext

import scalaz.Scalaz._
import scalaz._

class KanColleLacquer(wsServer: ActorRef) extends Lacquer {
  // URL書き換え関数
  // ElectricEyeにアクセスできる短縮URLを提供する
  val spoofElectricEyeURL: PartialFunction[RequestContext, RequestContext] =
    spoofURL(kanColleElectricEyeHookHostFrom)(kanColleElectricEyeHookHostTo)

  // リクエストの何も書き換えないダミー関数
  // 関係無いリクエストは無視させる
  val spoofNothing: PartialFunction[RequestContext, RequestContext] = {
    case anything: RequestContext ⇒ anything
  }

  // HTTPリクエストを受け付けるルーティング
  override def route = (ctx: RequestContext) ⇒ {
    // 特定のコンテキストを書き換える
    // ヒットしなければ何も書き換えない
    val spoofedCtx = ctx |> (spoofElectricEyeURL orElse spoofNothing)

    super.route(spoofedCtx)

    KanColle.extractRequest(ctx.request) match {
      case Some(genKanColleMessage: (HttpResponse ⇒ KanColleMessage)) ⇒
        // クライアントが要求するページをフェッチし、メッセージに変換してWebSocketで送信する
        fetch(ctx).onSuccess {
          case response: HttpResponse ⇒
            val wsMessage = genKanColleMessage(response)
            log.info(wsMessage.toString.take(100))
            wsServer ! wsMessage
        }
      case None ⇒
    }
  }
}
