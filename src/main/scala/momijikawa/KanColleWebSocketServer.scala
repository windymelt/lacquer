package momijikawa

import akka.actor.{ ActorSystem, Actor, Props, ActorLogging, ActorRef, ActorRefFactory }
import akka.io.IO
import momijikawa.KanColleWebSocketServer.Push
import momijikawa.lacquer.KanColleMessage.Port
import spray.can.Http
import spray.can.server.UHttp
import spray.can.websocket
import spray.can.websocket.frame.{ BinaryFrame, TextFrame }
import spray.http.HttpRequest
import spray.can.websocket.FrameCommandFailed
import spray.routing.HttpServiceActor

object KanColleWebSocketServer {
  final case class Push(msg: String)
  def props() = Props(classOf[KanColleWebSocketServer])
}

class KanColleWebSocketServer extends Actor with ActorLogging {

  var connections = List[ActorRef]()

  def receive = {
    case Http.Connected(remoteAddress, localAddress) ⇒
      log.info("ws connected")
      val serverConnection = sender()
      val conn = context.actorOf(KanColleWebSocketWorker.props(serverConnection))
      connections = connections :+ conn
      serverConnection ! Http.Register(conn)
    case Http.Closed ⇒
      connections = connections.filterNot(_ == sender())
    case message: Port ⇒
      connections.map {
        conn ⇒
          conn ! Push(message.jsonString)
      }
  }
}
