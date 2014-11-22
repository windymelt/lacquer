package momijikawa

import akka.actor.{ Props, ActorSystem }
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.server.UHttp
import scala.concurrent.duration._
import spray.can.Http
import spray.can.Http.ClientConnectionType

object Application extends App {
  override def main(args: Array[String]) = {
    implicit val timeout: Timeout = 1 minutes
    val wsSystem = ActorSystem("ws")
    val lacquerSystem = ActorSystem("lacquer")

    println("actorSystem, execContext initialized")

    println("setup created")

    val wsServer = wsSystem.actorOf(KanColleWebSocketServer.props(), "websocket")
    IO(UHttp)(wsSystem) ! Http.Bind(wsServer, "0.0.0.0", port = 8081)

    val httpProxyServer = lacquerSystem.actorOf(Props(classOf[KanColleLacquer], wsServer))
    IO(Http)(lacquerSystem) ! Http.Bind(httpProxyServer, "0.0.0.0", port = 8082)
  }
}
