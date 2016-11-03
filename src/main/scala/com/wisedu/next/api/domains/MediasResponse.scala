package com.wisedu.next.api.domains

import org.joda.time.DateTime

case class GetMediaResponse(status: String, errorMsg: String, name: String, orgType: String, srcType: String, depict: String,
                            img_url: String, back_img_url: String, ctime: DateTime, mtime: DateTime,
                            media_follow: String, feeds_count:Int, follow_count:Int,college_name:String)

case class GetMediaResp(self_url: String, name: String, summ: String, img_url: String, mtime: DateTime)

case class GetMediasResponse(medias: Seq[GetMediaResp])

case class GetCollegeMediaResp(self_url: String, name: String, summ: String, img_url: String, mtime: DateTime, media_follow: String)

case class GetCollegeMediasResponse(medias: Seq[GetCollegeMediaResp])

case class GetMediaFeedResp(self_url: String, title: String, ctime: DateTime, mtime: DateTime, read_num: Int,
                            update_num: Int, like_num: Int, view_style: Int, media_name: String, media_img_url: String,
                            summ: String, summ_img_url:String, summ_img_num: Int, online_user_num: Int, liveStatus: Int,
                            liveStartTime: DateTime, cont_url: String)

case class GetMediaFeedsResponse(status: String, errMsg:String, feeds: Option[Seq[GetMediaFeedResp]])

case class GetJsonResponse(result: String, msg: String)