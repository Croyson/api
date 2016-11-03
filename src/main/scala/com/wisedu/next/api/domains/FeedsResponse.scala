package com.wisedu.next.api.domains

import java.util.UUID
import org.joda.time.DateTime

case class GetChannelFeedResp(self_url: String, title: String, mtime: DateTime, ctime: DateTime, read_num: Long,
                              update_num: Long, like_num: Long, view_style: Int, media_name: String, media_img_url: String,
                              summ: String, summ_img_url: String, summ_img_num: Int, online_user_num:Long, liveStatus: Int,
                              liveStartTime: DateTime, src_url: String,videoLength:String, video_addr:String, video_type: String, cont_url: String, adv_img:String, list_img_type: Int)

case class GetChannelResponse(status: String, errMsg:String, feeds: Seq[GetChannelFeedResp])

case class GetChannelRsp(self_url: String, name: String, summ: String, img_url: String, sort_no: Int, group_type: Int)

case class GetChannelsResponse(channels: Seq[GetChannelRsp])

case class GetInvitersResp(user_id: UUID, user_img: String, user_url: String,user_sex:Int)

case class GetFeedInfoResponse(feed_id: UUID, media_id: String, media_name: String, media_img_url:String, media_summ: String,
                               media_follow: String, media_url:String, title: String,view_style: String, c_time: DateTime, m_time: DateTime,
                               src_url: String, user_like: String, summ: String, summ_img_url: String, video_addr: String, video_type: String,
                               cont_url: String, cont_src_type: Int, update_type: String, read_num: Long, like_num: Long, update_num: Long, group_name: String,
                               group_icon: String, group_desc: String, outer_page: String, result_url: String, finish_time: DateTime,
                               lottery_draw_title: String, inviters: Seq[GetInvitersResp], permit_emotions: Int, permit_thumbs_up: Int,is_emotion:String)

case class PutFeedShareResponse(share_id: String)

case class PostFeedReadResponse(msg: String)

case class GetPosterFeedsResp(self_url: String)

case class GetPosterFeedsResponse(feeds: Seq[GetPosterFeedsResp])


case class GetSimpleFeedInfoResponse(feed_id: UUID, media_id: String, media_name: String, media_img_url:String, media_summ: String,
                               media_follow: String, media_url:String, title: String,view_style: String, c_time: DateTime, m_time: DateTime,
                               src_url: String, user_like: String, summ: String, summ_img_url: String, video_addr: String, video_type: String,
                               cont_url: String, cont_src_type: Int, update_type: String, read_num: Long, like_num: Long, update_num: Long, group_name: String,
                               group_icon: String, group_desc: String, permit_emotions: Int, permit_thumbs_up: Int,is_emotion:String)

