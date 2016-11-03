package com.wisedu.next.api.services

import javax.inject.Inject

import com.google.inject.Singleton
import com.twitter.util.Future
import com.wisedu.next.api.domains._
import com.wisedu.next.api.filters.UserContext._
import com.wisedu.next.models.FeedLotteryDraw
import com.wisedu.next.services._
import org.joda.time.DateTime

/**
  * Version: 1.1
  * Author: pattywgm 
  * Time: 16/6/27 下午3:33
  * Desc:
  */

@Singleton
class TopicsService {
  @Inject var groupBaseService: GroupBaseService = _
  @Inject var feedBaseService: FeedBaseService = _
  @Inject var serviceFunctions: ServiceFunctions = _
  @Inject var feedAndStatsBaseService: FeedAndStatsBaseService = _
  @Inject var updateBaseService: UpdateBaseService = _
  @Inject var groupFeedBaseService: GroupFeedsBaseService = _
  @Inject var userBaseService: UserBaseService = _
  @Inject var mediaBaseService: ServiceBaseService = _
  @Inject var baseService: BaseService = _


  // 获取有奖话题
  def getPrizeTopic(request: GetPrizeTopicRequest): Future[GetPrizeTopicResponse] = {
    val collegeId = request.college_id.getOrElse(request.request.user.collegeId)
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)

    groupBaseService.getGroupByCollegeId(collegeId, "3", limits, offset).flatMap {
      case Some(group) =>
        feedAndStatsBaseService.collFeeds(group.groupId, "", "1", "", offset, limits, "").flatMap {
          feedAndStats => Future.collect(feedAndStats.map {
            feed =>
              feedBaseService.getFeedLotteryByFeedId(feed.feedId.toString).map {
                case Some(lottery) => serviceFunctions.toGetPrizeTopicResp(lottery, feed.title, feed.sImgUrl)
                case None => serviceFunctions.toGetPrizeTopicResp(
                  FeedLotteryDraw(feed.feedId.toString, "", "", "", "", "", DateTime.now, DateTime.now.plusDays(2)), feed.title, feed.sImgUrl)
              }
          })
        }.map(item => GetPrizeTopicResponse("success", "", item))
      case None => Future {
        GetPrizeTopicResponse("fail", "The channel does not exists!", List())
      }
    }
  }

  // 获取本周话题
  def getThisWeekTopics(request: GetThisWeekTopicRequest): Future[GetThisWeekTopicResponse] = {
    val collegeId = request.college_id.getOrElse(request.request.user.collegeId)
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)

    groupBaseService.getGroupByCollegeId(collegeId, "1", 1, 0).flatMap {
      case Some(group) =>
        feedAndStatsBaseService.collFeeds(group.groupId, "", "1", "", offset, limits, "").flatMap {
          feedAndStats => Future.collect(feedAndStats.map {
            feed => {
              val invitersF = baseService.collInviters(feed.topicInviter)
              val hotUpdatesF = updateBaseService.getHotUpdatesByFeedId(feed.feedId, limits).flatMap {
                updates => Future.collect(updates.map {
                  update => userBaseService.getUserById(update.userId).map {
                    case Some(user) =>
                      serviceFunctions.toUpdate(update, user.alias, user.imgUrl, user.sex.toString, user.userId.toString)
                    case None =>
                      serviceFunctions.toUpdate(update, " ", " ", " ", " ")
                  }
                })
              }
              for {
                inviters <- invitersF
                hotUpdates <- hotUpdatesF
              } yield {
                serviceFunctions.toTopic(feed, hotUpdates, inviters)
              }
            }
          }).map(items => GetThisWeekTopicResponse("success", "", items))
        }
      case None => Future {
        GetThisWeekTopicResponse("fail", "The channel does not exists!", List())
      }
    }
  }

}
