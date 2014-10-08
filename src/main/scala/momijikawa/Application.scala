package momijikawa

import akka.actor.{ Props, ActorSystem }
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import spray.can.Http
import spray.can.Http.ClientConnectionType

object Application extends App {
  override def main(args: Array[String]) = {
    implicit val timeout: Timeout = 5 seconds
    implicit val system = ActorSystem("lacquer")
    implicit val execContext = system.dispatcher

    println("actorSystem, execContext initialized")

    println("setup created")

    val service = system.actorOf(Props[Lacquer])
    IO(Http) ! Http.Bind(service, "0.0.0.0", port = 8082)
  }
}
