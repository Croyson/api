package com.wisedu.next.api.domains

import java.util.UUID

import org.joda.time.DateTime

/**
  * Version: 2.0
  * Author: pattywgm 
  * Time: 16/5/18 下午5:24
  * Desc:
  */


case class GetPrizeTopicResp(topic_id: UUID, title: String, outer_page: String, url: String, finish_time: DateTime, sImg: String)

case class GetPrizeTopicResponse(status: String, errMsg:String, topics: Seq[GetPrizeTopicResp])

case class Update(user_img: String, like_num: Long, user_name: String, imgs: String, content: String, user_sex: String, isAnonymousUser:String, update_type: Int, user_id: String)

case class Topic(topic_id: UUID, title: String, read_num: Long, update_num: Long, url: String, updates: Seq[Update], inviters: Seq[GetInvitersResp])

case class GetThisWeekTopicResponse(status: String, errMsg:String, topics: Seq[Topic])

case class GetTopicResponse(title: String, user_id: String, user_name: String, user_img: String, summ: String,
                            read_num: Long, update_num: Long, sImg_url: String, cont_url: String, outer_page: String,
                            result_url: String, finish_time: DateTime, lottery_draw_title: String, inviters: Seq[GetInvitersResp],is_emotion:String)