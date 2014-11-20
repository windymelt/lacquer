package momijikawa

import akka.actor._
import akka.agent.Agent
import akka.io.IO
import momijikawa.KanColleWebSocketServer.Push
import momijikawa.lacquer.KanColleMessage.{ ApiStart2, Port }
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
  implicit val execContext = context.dispatcher.prepare()

  var connections = List[ActorRef]()
  val apiStart2Cache = Agent[Option[ApiStart2]](None)
  val portCache = Agent[Option[Port]](None)

  def receive = {
    case Http.Connected(remoteAddress, localAddress) ⇒
      log.info("ws connected")
      val serverConnection = sender()
      val conn = context.actorOf(KanColleWebSocketWorker.props(serverConnection))
      connections = connections :+ conn
      serverConnection ! Http.Register(conn)
      context.watch(conn)
      // api_start2, portのキャッシュがあるときは、新規着信の扱いにしてデータをクライアントに送ってあげる。
      // クライアント再接続時のリロードの手間を省くため。
      apiStart2Cache().foreach {
        apiStart2 ⇒
          log.info("Re-sending api_start2...")
          Thread.sleep(200)
          conn ! Push(s"""{"api":"api_start2","data":${apiStart2.jsonString}}""")
      }
      portCache().foreach {
        port ⇒
          log.info("Re-sending port...")
          Thread.sleep(200)
          conn ! Push(s"""{"api":"port","data":${port.jsonString}}""")
      }
    case Http.Closed ⇒
      log.info("ws disconnected")
      connections = connections.filterNot(_ == sender())
    case Terminated(actor) ⇒
      // 切断したコネクションのアクターをリストから外す
      connections = connections.filterNot(_ == actor)

    case message: Port ⇒
      log.info("Updating port data...")
      portCache.alter { _ ⇒ Some(message) } foreach { msg: Option[Port] ⇒ log.info(s"Now, port cache is ${msg.toString.take(200)}") }
      connections.map {
        conn ⇒
          conn ! Push(s"""{"api":"port","data":${message.jsonString}}""")
      }
    case message: ApiStart2 ⇒
      log.info("Updating api_start2 data...")
      apiStart2Cache.alter { _ ⇒ Some(message) } foreach { msg: Option[ApiStart2] ⇒ log.info(s"Now, api_start2 cache is ${msg.toString.take(200)}") }
      connections.map {
        conn ⇒
          conn ! Push(s"""{"api":"api_start2","data":${message.jsonString}}""")
      }
  }
}
