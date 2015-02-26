package momijikawa

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.util.Timeout
import spray.can.Http
import spray.can.server.UHttp

import scala.concurrent.duration._

object Application extends App {
  override def main(args: Array[String]) = {
    implicit val timeout: Timeout = 1 minutes
    val wsSystem = ActorSystem("ws")
    val lacquerSystem = ActorSystem("lacquer")

    println("actorSystem, execContext initialized")

    println("setup created")

    // ブラウザアプリケーションにデータを送信するWebSocketを扱うサーバを作成、0.0.0.0:8081にバインドする
    val wsServer = wsSystem.actorOf(KanColleWebSocketServer.props(), "websocket")
    IO(UHttp)(wsSystem) ! Http.Bind(wsServer, "0.0.0.0", port = 8081)

    // 艦これの通信をキャプチャするプロキシを作成、0.0.0.0:8082にバインドする
    val httpProxyServer = lacquerSystem.actorOf(Props(classOf[KanColleLacquer], wsServer))
    IO(Http)(lacquerSystem) ! Http.Bind(httpProxyServer, "0.0.0.0", port = 8082)
  }
}
