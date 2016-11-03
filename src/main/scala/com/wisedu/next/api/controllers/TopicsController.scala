package com.wisedu.next.api.controllers

import java.util.UUID
import javax.inject.Inject

import com.twitter.finatra.annotations.Flag
import com.twitter.finatra.http.Controller
import com.wisedu.next.api.domains._
import com.wisedu.next.api.filters.UserContext._
import com.wisedu.next.api.services.{FeedsService, TopicsService}
import com.wisedu.next.services.FeedBaseService

/**
 * Version: 1.1
 * Author: pattywgm
 * Time: 16/5/20 上午9:35
 * Desc:
 */

class TopicsController extends Controller {

  @Inject
  @Flag("live.group") var liveGroupFlag: String = _

  @Inject var feedBaseService: FeedBaseService = _
  @Inject var feedService: FeedsService = _
  @Inject var topicService: TopicsService = _


  // 获取有奖话题
  get("/v2/prizeTopic") { request: GetPrizeTopicRequest =>
    topicService.getPrizeTopic(request).map {
      resp => response.ok.json(resp)
    }
  }

  // 获取本周话题
  get("/v2/thisWeekTopic") { request: GetThisWeekTopicRequest =>
    topicService.getThisWeekTopics(request).map {
      resp => response.ok.json(resp)
    }
  }

  // 获取话题详细信息
  get("/v2/topic/:id") { request: GetTopicRequest =>
    val feed_id = request.id
    val user_id = request.request.user.userId
    val read_type = request.read_type.toInt
    val src_value = request.src_value.getOrElse("")
    val push_value = request.push_value.getOrElse("")
    val ip_addr = request.ip_addr.getOrElse("")

    feedBaseService.getFeedById(UUID.fromString(feed_id)).flatMap {
      case Some(feed) =>
        feedService.getFeedInfos(feed, UUID.fromString(feed_id), user_id, read_type, src_value, push_value, ip_addr).map {
          case (userService, feedStat, group, updateType, feedLike, lottery, inviters, media, isEmotion) =>
            response.ok.json(GetTopicResponse(feed.title, media._1, media._2, media._3, feed.summ, feedStat.readNum,
              feedStat.updateNum, feed.sImgUrl, feed.contUrl, lottery.lotteryDrawUrl, lottery.lotteryDrawRstUrl,
              lottery.finishTime, lottery.lotteryDrawTitle, inviters, isEmotion))
        }
      case None => response.badRequest.json(Map("errMsg" -> "The topic does not exist!")).toFuture
    }
  }
}
