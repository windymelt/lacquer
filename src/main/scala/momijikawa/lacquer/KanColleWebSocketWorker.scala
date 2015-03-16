package momijikawa.lacquer

import akka.actor.{ ActorRef, ActorRefFactory, Props }
import akka.io.Tcp.{ Aborted, Closed }
import momijikawa.lacquer.KanColleWebSocketServer.Push
import spray.can.websocket
import spray.can.websocket.FrameCommandFailed
import spray.can.websocket.frame.{ BinaryFrame, TextFrame }
import spray.http.HttpRequest
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
      log.info("Worker is sending data...", msg)
      send(TextFrame(msg))

    case x: FrameCommandFailed ⇒
      log.error("frame command failed", x)

    case x: HttpRequest ⇒
    case Closed | Aborted ⇒
      context.stop(self)

    case unknown ⇒ log.warning("Worker has received unknown message: " + unknown.toString)
  }

  def businessLogicNoUpgrade: Receive = {
    implicit val refFactory: ActorRefFactory = context
    runRoute {
      getFromResourceDirectory("webapp")
    }
  }
}
