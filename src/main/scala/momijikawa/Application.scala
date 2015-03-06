package momijikawa

import akka.actor.{ ActorSystem, Props, ActorRef }
import akka.io.IO
import akka.util.Timeout
import spray.can.Http
import spray.can.server.UHttp

import scala.concurrent.duration._

object Application extends App {
  override def main(args: Array[String]) = runServer()
  def runServer() = {
    implicit val timeout: Timeout = 1 minutes

    val hostName = "0.0.0.0"
    val webSocketPort = 8081
    val proxyPort = 8082

    val wsSystem = ActorSystem("ws")
    val lacquerSystem = ActorSystem("lacquer")

    wsSystem.log.info("WebSocket system has started.")
    lacquerSystem.log.info("Proxy system has started.")

    val wsRef = runWebSocket(hostName, webSocketPort, wsSystem)
    runProxy(wsRef, hostName, proxyPort, lacquerSystem)
  }
  def runWebSocket(host: String, port: Int, system: ActorSystem) = {
    // ブラウザアプリケーションにデータを送信するWebSocketを扱うサーバを作成
    val wsServer = system.actorOf(KanColleWebSocketServer.props(), "websocket")
    IO(UHttp)(system) ! Http.Bind(wsServer, host, port = port)
    wsServer
  }
  def runProxy(webSocketServer: ActorRef, host: String, port: Int, system: ActorSystem) = {
    // 艦これの通信をキャプチャするプロキシを作成
    val httpProxyServer = system.actorOf(Props(classOf[KanColleLacquer], webSocketServer))
    IO(Http)(system) ! Http.Bind(httpProxyServer, host, port = port)
    httpProxyServer
  }
}
