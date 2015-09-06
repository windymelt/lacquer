package momijikawa.lacquer.kancolleTools

import akka.actor.{ ActorRef, ActorLogging, Actor }
import scala.concurrent.duration._

/**
 * 一定時間後にイベントを発生させるためのクラス
 */
class Timer extends Actor with ActorLogging {
  implicit val execContext = context.dispatcher.prepare()

  def receive = init
  def init: Receive = {
    case SetRecipient(ref)               ⇒ context.become(standby(ref))
    case allMessagesBeforeInitialization ⇒ // do nothing
  }
  def standby(recipient: ActorRef): Receive = {
    case Reservation(at, title, description) ⇒
      log.debug("Reservation received.")
      val duration = ((at - scala.compat.Platform.currentTime) / 1000) seconds;
      log.debug("The duration is " + duration.toString())
      context.system.scheduler.scheduleOnce(
        delay = duration,
        receiver = self,
        message = ItIsTheTime(title, description))
      recipient ! s"OK: タイマーをセットしました: ${duration.toString()}"
    case entireMessage @ ItIsTheTime(title, message) ⇒
      recipient ! entireMessage
  }
}
