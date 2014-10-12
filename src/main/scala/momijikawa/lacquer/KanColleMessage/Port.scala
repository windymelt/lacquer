package momijikawa.lacquer.KanColleMessage

import momijikawa.lacquer.KanColleMessage.KanColleMessage
import spray.http.{ HttpRequest, HttpResponse }
import scalaz._
import Scalaz._

case class Port(jsonString: String) extends KanColleMessage
/*
case class Port(api_result: Int, api_result_msg: String, api_data: ApiData) extends KanColleMessage {

}

case class ApiData(
                    api_marterial: List[ApiMaterial],
                    api_deck_port: List[ApiDeckPort],
                    api_ndock: List[ApiNDock],
                    api_ship: List[ApiShip],
                    api_basic: ApiBasic,
                    api_log: List[ApiLog],
                    api_combined_flag: Int,
                    api_p_bgm_id: Int)

case class ApiMaterial(api_material_id: Int, api_id: Int, api_value: Int)

case class ApiDeckPort(
                        api_member_id: Int,
                        api_id: Int,
                        api_name: String,
                        api_name_id: String,
                        api_mission: List[Int],
                        api_flagship: String,
                        api_ship: List[Int])

case class ApiNDock(
                     api_member_id: Int,
                     api_id: Int,
                     api_state: Int,
                     api_ship_id: Int,
                     api_complete_time: Int,
                     api_complete_time_str: String,
                     api_item1: Int,
                     api_item2: Int,
                     api_item3: Int,
                     api_item4: Int)

class ApiShip(
               val api_id: Int,
               val api_sortno: Int,
               val api_ship_id: Int,
               val api_lv: Int,
               val api_exp: List[Int],
               val api_nowhp: Int,
               val api_maxhp: Int,
               val api_leng: Int,
               val api_slot: List[Int],
               val api_onslot: List[Int],
               val api_kyouka: List[Int],
               val api_backs: Int,
               val api_fuel: Int,
               val api_bull: Int,
               val api_slotnum: Int,
               val api_ndock_time: Int,
               val api_ndock_item: List[Int],
               val api_srate: Int,
               val api_cond: Int,
               val api_karyoku: List[Int],
               val api_raisou: List[Int],
               val api_taiku: List[Int],
               val api_soukou: List[Int],
               val api_kaihi: List[Int],
               val api_taisen: List[Int],
               val api_sakuteki: List[Int],
               val api_lucky: List[Int],
               val api_locked: Int,
               val api_locked_equip: Int)

class ApiBasic(
                val api_member_id: String,
                val api_nickname: String,
                val api_nickname_id: String,
                val api_active_flag: Int,
                val api_starttime: BigInt,
                val api_level: Int,
                val api_rank: Int,
                val api_experience: Int,
                val api_fleetname: Option[String],
                val api_comment: String,
                val api_comment_id: String,
                val api_max_chara: Int,
                val api_max_slotitem: Int,
                val api_max_kagu: Int,
                val api_playtime: BigInt,
                val api_tutorial: Int,
                val api_furniture: List[Int],
                val api_count_deck: Int,
                val api_count_kdock: Int,
                val api_count_ndock: Int,
                val api_fcoin: Int,
                val api_st_win: Int,
                val api_st_lose: Int,
                val api_ms_count: Int,
                val api_ms_success: Int,
                val api_pt_win: Int,
                val api_pt_lose: Int,
                val api_pt_challenged: Int,
                val api_pt_challenged_win: Int,
                val api_firstflag: Int,
                val api_tutorial_progress: Int,
                val api_pvp: List[Int],
                val api_large_dock: Int)

case class ApiLog(api_no: Int, api_type: String, api_state: String, api_message: String)
*/
object PortConverter extends KanColleMessageConverter {
  import org.json4s._
  import org.json4s.native.JsonMethods._
  implicit val formats = DefaultFormats

  def apply(message: HttpRequest): \/[HttpResponse ⇒ KanColleMessage, HttpRequest] = {
    """kcsapi\/api_port\/port""".r.findFirstIn(message.uri.path.toString()) match {
      case Some(_) ⇒ -\/(response2Message)
      case None    ⇒ \/-(message)
    }
  }
  def response2Message(response: HttpResponse): Port = {
    val jsonString = response.entity.data.asString.substring(7)
    //parse(jsonString).extract[Port]
    Port(jsonString)
  }
}
