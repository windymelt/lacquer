package momijikawa.lacquer

import akka.actor._
import akka.agent.Agent
import KanColleMessage.{ ApiStart2, Port, Slot_item }
import spray.can.Http

object KanColleWebSocketServer {
  final case class Push(msg: String)
  def props() = Props(classOf[KanColleWebSocketServer])
}

class KanColleWebSocketServer extends Actor with ActorLogging {
  import KanColleWebSocketServer.Push
  implicit val execContext = context.dispatcher.prepare()

  var connections = List[ActorRef]()
  val apiStart2Cache = Agent[Option[ApiStart2]](None)
  val portCache = Agent[Option[Port]](None)
  val slot_itemCache = Agent[Option[Slot_item]](None)

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
      slot_itemCache().foreach {
        slot_item ⇒
          log.info("Re-sending slot_item...")
          Thread.sleep(200)
          conn ! Push(s"""{"api":"slot_item","data":${slot_item.jsonString}}""")
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
    case message: Slot_item ⇒
      log.info("Updating slot_item data...")
      slot_itemCache.alter { _ ⇒ Some(message) } foreach { msg: Option[Slot_item] ⇒ log.info(s"Now, slot_item cache is ${msg.toString.take(200)}") }
      connections.map {
        conn ⇒
          conn ! Push(s"""{"api":"slot_item","data":${message.jsonString}}""")
      }
  }
}
