package momijikawa.laquer.KanColleMessage

import momijikawa.laquer.KanColleMessage.KanColleMessage
import spray.http.{ HttpRequest, HttpResponse }
import scalaz._
import Scalaz._

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
  api_id: Int,
  api_sortno: Int,
  api_ship_id: Int,
  api_lv: Int,
  api_exp: List[Int],
  api_nowhp: Int, api_maxhp: Int, api_leng: Int, api_slot: List[Int], api_onslot: List[Int], api_kyouka: List[Int], api_backs: Int,
  api_fuel: Int, api_bull: Int, api_slotnum: Int, api_ndock_time: Int, api_ndock_item: List[Int], api_srate: Int, api_cond: Int,
  api_karyoku: List[Int], api_raisou: List[Int], api_taiku: List[Int], api_soukou: List[Int], api_kaihi: List[Int], api_taisen: List[Int],
  api_sakuteki: List[Int], api_lucky: List[Int], api_locked: Int, api_locked_equip: Int)
class ApiBasic(
  api_member_id: String,
  api_nickname: String,
  api_nickname_id: String,
  api_active_flag: Int,
  api_starttime: BigInt,
  api_level: Int, api_rank: Int, api_experience: Int, api_fleetname: Option[String], api_comment: String, api_comment_id: String,
  api_max_chara: Int, api_max_slotitem: Int, api_max_kagu: Int, api_playtime: BigInt, api_tutorial: Int, api_furniture: List[Int],
  api_count_deck: Int, api_count_kdock: Int, api_count_ndock: Int, api_fcoin: Int, api_st_win: Int, api_st_lose: Int,
  api_ms_count: Int, api_ms_success: Int, api_pt_win: Int, api_pt_lose: Int, api_pt_challenged: Int,
  api_pt_challenged_win: Int, api_firstflag: Int, api_tutorial_progress: Int, api_pvp: List[Int], api_large_dock: Int)
case class ApiLog(api_no: Int, api_type: String, api_state: String, api_message: String)

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
    parse(jsonString).extract[Port]
  }
}
