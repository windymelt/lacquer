package momijikawa.lacquer.KanColleMessage

import spray.http.HttpResponse

case class Slot_item(jsonString: String) extends KanColleMessage

object Slot_itemConverter extends KanColleMessageConverter {
  import org.json4s._
  implicit val formats = DefaultFormats

  val uriRegex = """kcsapi\/api_get_member\/slot_item"""

  def response2Message(response: HttpResponse): Slot_item = {
    Slot_item(getJsonString(response))
  }
}
