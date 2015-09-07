package momijikawa.lacquer.kancolleTools

import us.troutwine.barkety._
import akka.actor._
import akka.pattern.ask
import scala.concurrent.duration._

class XMPPClient extends Actor with ActorLogging {
  implicit val timeout: akka.util.Timeout = 20 seconds
  implicit val execContext = context.dispatcher.prepare()

  def receive = {
    case allMessageBeforeInitialization: String ⇒ // do nothing
    case ConnectXMPP(jid, password, toJid) ⇒
      log.debug("ConnectXMPP received.")
      val myJid = JID(jid)
      val chatsup = context.actorOf(Props(classOf[ChatSupervisor], myJid, password, None, None), "XMPP_ChatSupervisor")
      log.debug("ChatSupervisor created.")
      (chatsup ? CreateChat(JID(toJid))) foreach {
        case chatter: ActorRef ⇒
          log.debug("Chatter has been created.")
          //actorOf(new Acty(chatter)).start
          context.become({
            case str: String ⇒
              chatter ! OutboundMessage(str)
          })
          self ! context.system.settings.config.getString("lacquer.xmpp.on-boot-message")
        case unknown ⇒ log.error("unknown message received: " + unknown.toString)
      }
  }
}
