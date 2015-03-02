package momijikawa.lacquer.KanColleMessage

import spray.http.{ HttpRequest, Uri, HttpResponse }

import scalaz.{ \/-, -\/, \/ }

trait KanColleMessageConverter {
  val uriRegex: String
  // レスポンスの文字列から"svdata="を削除して返す
  def getJsonString(response: HttpResponse): String = response.entity.data.asString.substring(7)
  def uriContainsString(uri: Uri, regex: String) = regex.r.findFirstIn(uri.path.toString()).isDefined

  def apply(message: HttpRequest): \/[HttpResponse ⇒ KanColleMessage, HttpRequest] = {
    uriContainsString(message.uri, uriRegex) match {
      case true  ⇒ -\/(response2Message)
      case false ⇒ \/-(message)
    }
  }
  def response2Message(response: HttpResponse): KanColleMessage
}
