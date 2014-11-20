package momijikawa

import akka.actor.ActorRef
import BasicLacquerConfiguration._
import momijikawa.lacquer.KanColle
import momijikawa.lacquer.KanColleMessage.KanColleMessage
import spray.http.{ HttpHeaders, HttpHeader, Uri, HttpResponse }
import spray.routing.RequestContext
import scalaz._
import Scalaz._

class KanColleLacquer(wsServer: ActorRef) extends Lacquer {
  val spoofElectricEyeURL: PartialFunction[RequestContext, RequestContext] = {
    case c: RequestContext if c.request.uri.authority.host.address == kanColleElectricEyeHookHostFrom ⇒
      // ヘッダリストの中の"Host"ヘッダのみを書き換えるヘルパー関数
      val modHeader: List[HttpHeader] ⇒ List[HttpHeader] = _.map {
        case header if header.name == "Host" ⇒
          // 実際にアクセスするGitHub上のホストを"Host"ヘッダとして使わせる
          HttpHeaders.Host(host = kanColleElectricEyeHookHostTo)
        case other ⇒ other
      }

      // 特定のURLをGithub上の艦これツール本体へのURLに書き換えて、ヘッダを書き換えてエラーを回避する
      // Sprayはこのリクエストに従ってGithubへアクセスする
      c.copy(request = c.request.copy(
        uri = Uri(kanColleElectricEyePageURL + c.request.uri.path.toString()),
        headers = modHeader(c.request.headers)))
  }

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
