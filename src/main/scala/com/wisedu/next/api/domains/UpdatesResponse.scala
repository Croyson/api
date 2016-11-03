package com.wisedu.next.api.domains

import java.util.UUID

import org.joda.time.DateTime

/**
  * Version: 2.0
  * Author: croyson
  * Time: 16/6/15 下午5:23
  * Desc:
  */

case class UpdateOpResp(status: String, errorMsg: String)

case class GetUpdatesResp(self_url: String, user_id: String, user_name: String,user_real_name: String,user_fresh_year:String, user_img_url: String, user_url: String,
                          user_college: String,user_college_shortName: String, user_depart: String, user_grade: String, likes: Long, content: String,
                          user_sex: String, cTime: DateTime, sub_update_num: Long, img_url: String, is_like: String, is_emotion: String,
                          emotions: Seq[GetEmotionsResp], like_users: Seq[GetEmotionsUserResp], like_num: Long, isAnonymousUser: String,
                          update_type: Int, thresh_hold: Int,reply_msg_id:String,reply_user_id:String,reply_user_name:String)

case class GetUpdatesResponse(status: String, errMsg:String, updates: Seq[GetUpdatesResp])

case class GetUpdatesTopResp(self_url: String, user_name: String,user_real_name: String,user_fresh_year:String, user_img_url: String, user_url: String,
                             user_college: String, user_depart: String, user_grade: String, likes: Long,
                             content: String, user_sex:String, c_time:DateTime, is_like: String, is_emotion: String, isAnonymousUser: String, user_id: String)

case class GetUpdatesTopResponse(updates: Seq[GetUpdatesTopResp])

case class PostUpdatesResp(feed_id: UUID, update_id: UUID, content: String, img_urls: String)

case class PostUpdatesResponse(status:String, errMsg:String, update: Option[PostUpdatesResp])

case class GetFeedUpdateResp(update_id: UUID, feed_id: UUID, user_id: UUID, user_name: String,user_real_name: String,user_fresh_year:String, user_img_url: String,user_college: String,user_college_shortName: String,user_depart: String, user_sex: String,content: String, c_time: DateTime,
                             like_num: Long, unlike_num: Long, p_update_id: UUID, update_level: Int, img_urls: String, is_like: String,is_emotion: String, emotions: Seq[GetEmotionsResp],
                             like_users: Seq[GetEmotionsUserResp], isAnonymousUser: String,update_type:Int, thresh_hold: Int)

case class GetFeedUpdateResponse(update: GetFeedUpdateResp)

case class FeedUpdateResp(status: String, errorMsg: String,datas:Option[GetFeedUpdateResponse])

case class GetUpdateModeResp(value: Int, descri: String)

case class GetUpdateModeResponse(modes: Seq[GetUpdateModeResp], update_type: Int)

case class GetMsgUpdatesResp(self_url: String,msg_id:UUID, user_id: String, user_name: String,user_real_name: String,user_fresh_year:String, user_sex: Int, user_img: String, user_url: String,
                          user_college: String,college_short_name: String, depart_short_name: String, user_depart: String, user_grade: String, is_anonymous: Int, like_num: Long, content: String,
                             img_url: String, c_time: DateTime, sub_update_num: Long, is_like: String,is_emotion: String, update_type: Int, thresh_hold: Int,
                          emotions: Seq[GetEmotionsResp], like_users: Seq[GetEmotionsUserResp],reply_msg_id:String,reply_user_id:String,reply_user_name:String)

case class GetMsgUpdatesResponse(status: String, errMsg: String, updates: Seq[GetMsgUpdatesResp])
