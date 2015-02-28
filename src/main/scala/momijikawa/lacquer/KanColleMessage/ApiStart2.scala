package momijikawa.lacquer.KanColleMessage

import spray.http.{ HttpRequest, HttpResponse }

import scalaz._

case class ApiStart2(jsonString: String) extends KanColleMessage

object ApiStart2Converter extends KanColleMessageConverter {
  import org.json4s._
  implicit val formats = DefaultFormats

  val uriRegex = """kcsapi\/api_start2"""

  def response2Message(response: HttpResponse): ApiStart2 = {
    ApiStart2(getJsonString(response))
  }
}
