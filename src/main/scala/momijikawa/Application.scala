package momijikawa

import akka.actor._
import akka.io.IO
import akka.util.Timeout
import spray.can.Http
import spray.can.server.UHttp
import com.typesafe.config._

import scala.concurrent.duration._

object Application extends App {
  override def main(args: Array[String]) = runServer(ConfigFactory.defaultReference())
  def runServer(config: Config) = {
    implicit val timeout: Timeout = 1 minutes

    config.checkValid(ConfigFactory.defaultReference(), "lacquer")

    val hostName = config.getString("lacquer.host")
    val webSocketPort = config.getInt("lacquer.websocket.port")
    val proxyPort = config.getInt("lacquer.proxy.port")

    val wsSystem = ActorSystem("ws")
    val lacquerSystem = ActorSystem("lacquer")

    wsSystem.log.info("WebSocket system has started.")
    lacquerSystem.log.info("Proxy system has started.")

    val wsRef = runWebSocket(hostName, webSocketPort, wsSystem)
    val proxyRef = runProxy(wsRef, hostName, proxyPort, lacquerSystem)

    (wsSystem, wsRef, lacquerSystem, proxyRef)
  }
  def runWebSocket(host: String, port: Int, system: ActorSystem) = {
    // ブラウザアプリケーションにデータを送信するWebSocketを扱うサーバを作成
    system.log.info(s"Preparing websocket on $host:$port ...", host, port)
    val wsServer = system.actorOf(KanColleWebSocketServer.props(), "websocket")
    IO(UHttp)(system) ! Http.Bind(wsServer, host, port = port)
    wsServer
  }
  def runProxy(webSocketServer: ActorRef, host: String, port: Int, system: ActorSystem) = {
    // 艦これの通信をキャプチャするプロキシを作成
    system.log.info(s"Preparing HTTP proxy on $host:$port ...", host, port)
    val httpProxyServer = system.actorOf(Props(classOf[KanColleLacquer], webSocketServer))
    IO(Http)(system) ! Http.Bind(httpProxyServer, host, port = port)
    httpProxyServer
  }
  def stopServer(wsSystem: ActorSystem, wsRef: ActorRef, proxySystem: ActorSystem, proxyRef: ActorRef) = {
    wsRef ! PoisonPill
    wsSystem.awaitTermination()
    println("Websocket uptime: " + wsSystem.uptime)
    wsSystem.shutdown()

    proxyRef ! PoisonPill
    proxySystem.awaitTermination()
    println("Proxy uptime: " + proxySystem.uptime)
    proxySystem.shutdown()
  }
}
