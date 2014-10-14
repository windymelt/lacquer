package momijikawa.lacquer

import momijikawa.lacquer.KanColleMessage.{ ApiStart2Converter, KanColleMessage, PortConverter }
import spray.http.{ HttpRequest, HttpResponse }
import scalaz._
import Scalaz._

object KanColle {
  def kanColleAnalyze(message: HttpRequest): Option[HttpResponse ⇒ KanColleMessage] = {
    if ("/kcsapi/".r.findFirstIn(message.uri.path.toString()).isEmpty) { return None }
    PortConverter(message) >> ApiStart2Converter(message) match {
      case -\/(converter) ⇒ Some(converter)
      case \/-(_)         ⇒ None
    }
  }
  def kanColleMainWindow: String = {
    """<html>
      |
    """.stripMargin
  }
}
