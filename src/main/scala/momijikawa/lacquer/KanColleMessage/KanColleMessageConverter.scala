package momijikawa.lacquer.KanColleMessage

import spray.http.HttpResponse

trait KanColleMessageConverter {
  // レスポンスの文字列から"svdata="を削除して返す
  def getJsonString(response: HttpResponse): String = response.entity.data.asString.substring(7)
}
