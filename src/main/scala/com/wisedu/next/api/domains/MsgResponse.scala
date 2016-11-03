package com.wisedu.next.api.domains

import java.util.UUID

import org.joda.time.DateTime

/**
  * Version: 1.1
  * Author: pattywgm 
  * Time: 16/7/6 上午9:12
  * Desc: 新鲜事(消息)请求返回
  */

case class GetMsgPosterResp(user_id: String, user_url: String,  user_name: String, user_real_name: String,user_fresh_year:String, user_img: String, user_college: String, user_depart: String, user_sex: Int, is_anonymous: Int, college_short_name: String, depart_short_name: String)

case class GetReferMsgResp(msg_id: String, msg_url: String, content: String, msg_img: String, user_id: String, user_url: String,  user_name: String,user_real_name: String,user_fresh_year:String)

case class GetMsgInfoResp(msg_id: String, content: String, msg_img: String, c_time: DateTime, msg_circle: String, group_id:String,circle_iconUrl:String,
                          like_num: Long, unlike_num: Long, update_num:Long, msg_type: String, is_like: String, is_emotion: String, referenced_msg: GetReferMsgResp,
                          user: GetMsgPosterResp, emotions: Seq[GetEmotionsResp], like_users: Seq[GetEmotionsUserResp],
                          update_type: Int, thresh_hold: Int, is_anonymous: Int,is_attention:Int,is_recommend:Int,adminUser:String,is_delete:Int)

case class GetMsgInfoExpressResp(msg_id: String,like_num: Long, unlike_num: Long, emotions: Seq[GetEmotionsResp],
                                 like_users: Seq[GetEmotionsUserResp])

case class GetMsgInfoExpressResponse(status: String, errMsg:String, express: Option[GetMsgInfoExpressResp])

case class GetMsgInfoResponse(status: String, errMsg:String, msg: Option[GetMsgInfoResp])

case class GetMsgEmotionsResp(model_id: String, select_id:String, select_img: String, count: Int)

case class GetMsgHotUpdatesResp(update_id: String, update_content: String, update_img: String, user_id: String,
                                user_url: String, is_anonymous: Int, user_name: String,user_real_name: String,
                                user_fresh_year:String, update_type: Int, thresh_hold: Int,
                                reply_msg_id:String,reply_user_id:String,reply_user_name:String)

case class GetCircleMsgsResp(msg_id: String, msg_url: String, content: String, msg_circle: String, group_id:String,circle_iconUrl:String, msg_img: String,
                             msg_type: String, c_time: DateTime, update_num: Long, like_num: Long, is_like: String, is_emotion: String,
                             is_anonymous: Int, update_type: Int, thresh_hold: Int, referenced_msg: GetReferMsgResp, emotions: Seq[GetMsgEmotionsResp],
                             user: GetMsgPosterResp, updates: Seq[GetMsgHotUpdatesResp],is_top:Int,is_attention:Int,is_recommend:Int,is_delete:Int)


case class GetCircleMsgsResponse(status: String, errMsg:String, msg_list:Seq[GetCircleMsgsResp])

case class GetAllCircleMsgsResponse(status: String, errMsg:String, msg_list:Seq[GetCircleMsgsResp],isRecommend:String)

//case class PostMsgResp(msg_id:UUID,user_id:UUID,user_name:UUID,msg_content:String,msg_imgs:String,reply_msg_id:String,reply_user_id:String,reply_user_name:String)

case class PostMsgResponse(status:String, errMsg:String,postMsgResp:Option[GetMsgUpdatesResp])

case class GetFollowedUserResp(user_id:String,user_name:String,user_real_name: String,user_fresh_year:String,user_img: String,user_sex:String,user_college: String,college_short_name: String,
                               depart_short_name: String, user_depart: String, is_followed: Int)

case class GetFollowedUserResponse(status: String, errMsg:String, users:Option[Seq[GetFollowedUserResp]])

case class PostRecommendMsgResponse(status:String,errCode:String, errMsg:String)


case class UserMsgNoticeResp(msg_id: UUID, c_User_id: String,c_time: DateTime, content: String, img_urls: String, circle_id: String,
                             op_user: GetMsgPosterResp, op_time: DateTime, op_msgId: String, op_content: String, op_img_urls: String,
                             op_type: String,main_msg_id:UUID,isNew:Int)

case class GetUserNoticeListResp(status:String,errCode:String, errMsg:String,userMsgNotices:Seq[UserMsgNoticeResp])

case class GetUserMsgCountResp(status:String,errCode:String, errMsg:String,msgCount:Option[Long])

case class GetUserStatResp(user_id:String,is_attendtion:Int,is_each_attention:Int,follow_count:Long,fans_count:Long,msg_count:Long)


case class GetUserStatResponse(status:String,errCode:String, errMsg:String,userStat:Option[GetUserStatResp])


case class GetUserFocusStateResponse(status: String, errMsg:String, state:Int)