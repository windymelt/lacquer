package momijikawa.lacquer

import akka.actor._
import KanColleLacquerUtil._
import BasicLacquerConfiguration._
import KanColleMessage.KanColleMessage
import momijikawa.lacquer.kancolleTools.{ ConnectXMPP, XMPPClient }
import spray.http.HttpResponse
import spray.routing.RequestContext
import com.typesafe.config.Config

import scalaz.Scalaz._

class KanColleLacquer(wsServer: ActorRef) extends Lacquer {
  val system = ActorSystem("KanColleLacquer")
  val config = system.settings.config
  val xmpp = system.actorOf(Props[XMPPClient], "xmppclient")
  if (config.getBoolean("lacquer.xmpp.enabled")) {
    xmpp ! ConnectXMPP(
      jid = config.getString("lacquer.xmpp.user"),
      password = config.getString("lacquer.xmpp.password"),
      toJid = config.getString("lacquer.xmpp.recipient"))
  }
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
