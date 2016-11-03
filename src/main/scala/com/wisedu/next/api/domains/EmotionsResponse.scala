package com.wisedu.next.api.domains

/**
  * Version: 1.1
  * Author: pattywgm 
  * Time: 16/6/28 下午1:42
  * Desc:
  */

case class PostEmotionsResponse(result: String,status:String, errMsg:String)

case class GetEmotionsUserResp(user_id:String,user_name:String,user_real_name: String,user_fresh_year:String,user_img: String,user_sex:String,user_college: String,college_short_name: String,
                               depart_short_name: String, user_depart: String)

case class GetEmotionsResp(model_id: String, select_id:String, select_img: String, user: Seq[GetEmotionsUserResp], count: Int)

case class GetEmotionsResponse(status: String, errMsg:String, emotions: Seq[GetEmotionsResp], like_users: Seq[GetEmotionsUserResp], like_num: Long)

case class GetEmotionListResponse(status: String, errMsg:String,users:Option[Seq[GetEmotionsUserResp]])
