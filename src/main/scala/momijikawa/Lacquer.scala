package momijikawa

import akka.actor.Actor.Receive
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import spray.http.HttpResponse
import spray.routing._

class Lacquer(connector: ActorRef) extends HttpServiceActor {
  implicit val timeout: Timeout = Timeout(5 seconds)

  val route: Route = (ctx: RequestContext) => ctx.complete(connector.ask(ctx.request).mapTo[HttpResponse])

  def receive: Receive = runRoute(route)
}
