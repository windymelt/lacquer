package momijikawa

import akka.actor.{ ActorSystem, Actor, Props, ActorLogging, ActorRef, ActorRefFactory }
import akka.io.IO
import momijikawa.KanColleWebSocketServer.Push
import spray.can.Http
import spray.can.server.UHttp
import spray.can.websocket
import spray.can.websocket.frame.{ BinaryFrame, TextFrame }
import spray.http.HttpRequest
import spray.can.websocket.FrameCommandFailed
import spray.routing.HttpServiceActor

object KanColleWebSocketWorker {
  def props(serverConnection: ActorRef) = Props(classOf[KanColleWebSocketWorker], serverConnection)
}

class KanColleWebSocketWorker(val serverConnection: ActorRef) extends HttpServiceActor with websocket.WebSocketServerWorker {
  override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

  def businessLogic: Receive = {
    case x @ (_: BinaryFrame | _: TextFrame) ⇒
      sender ! x

    case Push(msg) ⇒
      log.info("sending", msg)
      send(TextFrame(msg))

    case x: FrameCommandFailed ⇒
      log.error("frame command failed", x)

    case x: HttpRequest ⇒

  }

  def businessLogicNoUpgrade: Receive = {
    implicit val refFactory: ActorRefFactory = context
    runRoute {
      getFromResourceDirectory("webapp")
    }
  }
}
