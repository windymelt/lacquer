package momijikawa.lacquer

import momijikawa.lacquer.KanColleLacquerUtil._
import org.specs2.mutable._
import spray.http.HttpHeaders

class KanColleLacquerUtilSpec extends Specification {
  "modHeader" should {
    "List[HttpHeader]に\"Host\"ヘッダが含まれていた場合は、任意の文字列でHostヘッダの内容を書き換える" in {
      val hostHeader = HttpHeaders.Host("host.example.com")
      val httpHeaders = List(hostHeader)
      val replacedHost: String = "replacedhost.example.com"
      modHeader(replacedHost)(httpHeaders).head.value should_== replacedHost
    }

    "List[HttpHeader]に\"Host\"ヘッダが含まれない場合は、何も変更しない" in {
      val contentLength = 1000
      val contentLengthHeader = HttpHeaders.`Content-Length`(contentLength)
      val httpHeaders = List(contentLengthHeader)
      val replacedHost: String = "replacedhost.example.com"
      modHeader(replacedHost)(httpHeaders) should_== httpHeaders
    }
  }
}
