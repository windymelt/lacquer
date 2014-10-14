package momijikawa

import akka.actor.ActorRef
import momijikawa.lacquer.KanColle
import momijikawa.lacquer.KanColleMessage.KanColleMessage
import spray.http.HttpResponse
import spray.routing.RequestContext

class KanColleLacquer(wsServer: ActorRef) extends Lacquer {
  override def route = (ctx: RequestContext) ⇒ {
    val analyze = KanColle.kanColleAnalyze(ctx.request)
    super.route(ctx)

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
