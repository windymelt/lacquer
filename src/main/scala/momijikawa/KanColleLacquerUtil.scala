package momijikawa

import spray.http.{ Uri, HttpHeaders, HttpHeader }
import spray.routing.RequestContext

object KanColleLacquerUtil {
  // ヘッダリストの中の"Host"ヘッダのみを書き換えるヘルパー関数
  val modHeader: String ⇒ List[HttpHeader] ⇒ List[HttpHeader] = toHost ⇒ _.map {
    case header if header.name == "Host" ⇒
      // 実際にアクセスするGitHub上のホストを"Host"ヘッダとして使わせる
      HttpHeaders.Host(host = toHost)
    case other ⇒ other
  }

  val spoofURL: String ⇒ String ⇒ PartialFunction[RequestContext, RequestContext] = fromHost ⇒ toHost ⇒ {
    case c: RequestContext if c.request.uri.authority.host.address == fromHost ⇒
      // 特定のホストへのアクセスリクエストを別のホストへのリクエストに書き換えて、ヘッダを書き換えてエラーを回避する
      // Sprayはこのリクエストに従ってGithubへアクセスする
      c.copy(request = c.request.copy(
        uri = Uri(toHost + c.request.uri.path.toString()),
        headers = modHeader(toHost)(c.request.headers)))
  }
}
