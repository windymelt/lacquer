package momijikawa.lacquer

import momijikawa.lacquer.KanColleMessage.{ApiStart2Converter, KanColleMessage, PortConverter}
import spray.http.{HttpRequest, HttpResponse}

import scalaz._
import Scalaz._

object KanColle {
  def kanColleAnalyze(message: HttpRequest): Option[HttpResponse ⇒ KanColleMessage] = {
    // "/kcsapi/"が含まれていなければ弾く
    if ("/kcsapi/".r.findFirstIn(message.uri.path.toString()).isEmpty) { return None }

    // port, apistart2の順に合致するか調べる
    PortConverter(message) >> ApiStart2Converter(message) match {
      case -\/(converter: (HttpResponse ⇒ KanColleMessage)) ⇒ Some(converter)
      case \/-(_) ⇒ None
    }
  }
  def kanColleMainWindow: String = {
    """<html>
      |
    """.stripMargin
  }
}
