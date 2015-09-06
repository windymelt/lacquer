package momijikawa.lacquer

import akka.actor._
import KanColleLacquerUtil._
import BasicLacquerConfiguration._
import KanColleMessage.KanColleMessage
import momijikawa.lacquer.kancolleTools.{ SetRecipient, ConnectXMPP, XMPPClient, Timer }
import spray.http.HttpResponse
import spray.routing.RequestContext

import scalaz.Scalaz._

class KanColleLacquer(wsServer: ActorRef) extends Lacquer {
  val system = ActorSystem("KanColleLacquer")
  val config = system.settings.config
  val xmpp = system.actorOf(Props[XMPPClient], "xmppclient")
  initXMPP()
  val timer = system.actorOf(Props[Timer], "timer")
  initTimer()

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

    // タイマーの要求があれば処理する
    val handleTimerResult = handleTimerRequest(ctx)
    if (handleTimerResult.isEmpty) {
      super.route(spoofedCtx)
    } else {
      ctx.complete(handleTimerResult.get)
    }

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

  private def initXMPP() = {
    if (config.getBoolean("lacquer.xmpp.enabled")) {
      xmpp ! ConnectXMPP(
        jid = config.getString("lacquer.xmpp.user"),
        password = config.getString("lacquer.xmpp.password"),
        toJid = config.getString("lacquer.xmpp.recipient"))
    }
  }

  private def initTimer() = {
    class TimerToXMPP extends Actor {
      def receive = {
        case kancolleTools.ItIsTheTime(title, description) ⇒ xmpp ! s"$title: $description"
      }
    }
    val recipientActor = system.actorOf(Props(new TimerToXMPP()), "timerRecipient")
    timer ! SetRecipient(recipientActor)
  }

  private def handleTimerRequest(ctx: RequestContext): Option[HttpResponse] = {
    import spray.http.{ HttpRequest, HttpHeaders, AllOrigins }
    import spray.json._
    object MyJsonProtocol extends DefaultJsonProtocol {
      implicit val ReservationFormat = jsonFormat3(kancolleTools.Reservation)
    }
    import MyJsonProtocol._

    if (ctx.request.uri.authority.host.address == "kcl.it" && ctx.request.uri.path.toString() == "/settimer") {
      ctx.request match {
        case HttpRequest(method, uri, headers, entity, protocol) if method.value == "POST" ⇒
          val reservation = entity.data.asString.parseJson.convertTo[kancolleTools.Reservation]
          timer ! reservation
          Some(HttpResponse(entity = "{status: \"ok\"}"))
        case HttpRequest(method, _, _, _, _) if method.value == "OPTIONS" ⇒
          Some(HttpResponse(headers = List(HttpHeaders.`Access-Control-Allow-Origin`(AllOrigins), HttpHeaders.`Access-Control-Allow-Headers`("content-type"))))
        case unknown ⇒
          log.error("handletimer: unknown request: " + unknown.toString)
          None
      }
    } else None
  }
}
