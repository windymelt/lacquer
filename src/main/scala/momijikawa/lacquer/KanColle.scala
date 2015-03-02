package momijikawa.lacquer

import momijikawa.lacquer.KanColleMessage.{ ApiStart2Converter, KanColleMessage, PortConverter }
import spray.http.{ HttpRequest, HttpResponse, Uri }

import scalaz._
import Scalaz._

object KanColle {
  def extractRequest(message: HttpRequest): Option[HttpResponse ⇒ KanColleMessage] = {
    // "/kcsapi/"が含まれていなければ弾く
    if (!isKanColleUri(message.uri)) { return None }

    // port, apistart2の順に合致するか調べる
    val combinedConverter = PortConverter(message) >> ApiStart2Converter(message)
    combinedConverter match {
      case -\/(converter: (HttpResponse ⇒ KanColleMessage)) ⇒ Some(converter)
      case \/-(_) ⇒ None
    }
  }

  private def isKanColleUri(uri: Uri) = "/kcsapi/".r.findFirstIn(uri.path.toString()).isDefined

  def kanColleMainWindow: String = {
    """<html>
      |
    """.stripMargin
  }
}
