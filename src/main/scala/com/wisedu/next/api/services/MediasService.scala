package com.wisedu.next.api.services

import com.google.inject.{Inject, Singleton}
import com.twitter.util.Future
import com.wisedu.next.api.domains._
import com.wisedu.next.api.filters.UserContext._
import com.wisedu.next.models.{College, FeedStat}
import com.wisedu.next.services.{CollegeBaseService, FeedBaseService, ServiceBaseService, UserBaseService}
import org.joda.time.DateTime


/**
  * Version: 1.1
  * Author: pattywgm 
  * Time: 16/6/23 下午2:20
  * Desc:
  */
@Singleton
class MediasService {

  @Inject var baseService: BaseService = _
  @Inject var userBaseService: UserBaseService = _
  @Inject var serviceBaseService: ServiceBaseService = _
  @Inject var serviceFunctions: ServiceFunctions = _
  @Inject var feedBaseService: FeedBaseService = _
  @Inject var collegeBaseService: CollegeBaseService = _


  // 获取媒体列表
  def getMediasList(request: GetMediasRequest): Future[GetMediasResponse] = {
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)
    val user_id = request.request.user.userId

    userBaseService.getSerByUser(user_id, limits, offset).map {
      servs => GetMediasResponse(servs.map(serv => serviceFunctions.toGetMediasResponse(serv)))
    }
  }

  // 获取学校相关或不相关的媒体列表
  def getCollegeMedias(request: GetMediasRequest, ifRelative: String): Future[GetCollegeMediasResponse] = {
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)
    val college_id = request.request.user.collegeId
    val user_id = request.request.user.userId


    serviceBaseService.collServicesByCollegeId(college_id, ifRelative, limits, offset).flatMap {
      services => Future.collect(services.map {
        media => userBaseService.checkUserLookService(user_id, media.serId).map {
          str => serviceFunctions.toGetCollegeMediasResponse(media, str)
        }
      }).map(item => GetCollegeMediasResponse(item))
    }
  }

  // 关注媒体 或 取消关注媒体
  def followMedias(request: PutMediaRequest, ifFollow: String): Future[Boolean] = {
    val userId = request.request.user.userId
    val mediaId = request.media_id
    try {
      if (ifFollow == "follow") {
        val userService = serviceFunctions.toUserService(userId, mediaId)
        userBaseService.insUserService(userService).map {
          result => true
        }
      } else {
        userBaseService.delUserService(userId, mediaId).map {
          result => true
        }
      }
    } catch {
      case _: Exception => Future(false)
    }
  }

  // 获取媒体详情
  def getMediaInfo(request: GetMediaRequest): Future[GetMediaResponse] = {
    val media_id = request.media_id
    val user_id = request.request.user.userId
    serviceBaseService.getServiceById(media_id).flatMap {
      case Some(media) =>{
        val userLookF = userBaseService.checkUserLookService(user_id, media_id)
        val collegeF = collegeBaseService.getCollegeById(media.collegeId).map(_.getOrElse(College("","","","","",0,0,"",0L,"",0)))
        val feedsCountF = feedBaseService.collFeedSize("","",media.serId,"","","1","")
        val followCountF = userBaseService.collUserServiceSize("",media.srcId)
        for{
          userLook <- userLookF
          college <- collegeF
          feedsCount <- feedsCountF
          followCount <- followCountF
        }yield (userLook, college.name, feedsCount, followCount)
      }.map{
        case (userLook, name, feedsCount, followCount) =>
          serviceFunctions.toGetMediaInfo("success","",media, userLook, name, feedsCount, followCount)
      }
      case None =>
        Future{GetMediaResponse("fail", "The media does not exists", "", "", "", "", "", "",
          DateTime.now, DateTime.now, "", 0, 0, "")}
    }
  }


  //获取媒体下的资讯集合
  def getMediaFeeds(request: GetMediaFeedsRequest):Future[GetMediaFeedsResponse] = {
    val media_id = request.media_id
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)

    serviceBaseService.getServiceById(media_id).flatMap {
      case Some(item) =>
        feedBaseService.getFeedsBySerId(media_id, "1", limits, offset).flatMap {
          feeds =>
            Future.collect(feeds.map {
              feed => feedBaseService.getFeedStatsById(feed.feedId).map {
                feedStat => serviceFunctions.toGetFeedsBySerIdResp(feed, feedStat.getOrElse(FeedStat(feed.feedId, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)), item)
              }
            })
        }.map(items => GetMediaFeedsResponse("success","",Some(items)))
      case None => Future{GetMediaFeedsResponse("fail","The media does not exists!", None)}
    }
  }

  // 获取已关注媒体下的所有资讯集合
  def getFollowedMediaFeeds(request: GetMediasRequest): Future[GetMediaFeedsResponse] ={
    val user_id = request.request.user.userId
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)

    feedBaseService.collFeedsByUserId(user_id, 1, limits, offset).flatMap {
      feeds => Future.collect(feeds.map {
        feed => {
          val mediaF = baseService.getServiceById(feed.serId)
          val feedStatF = baseService.getFeedStatById(feed.feedId)
          for {
            media <- mediaF
            feedStat <- feedStatF
          } yield serviceFunctions.toGetFeedsBySerIdResp(feed, feedStat, media)
        }

      }).map(items => GetMediaFeedsResponse("success","",Some(items)))
    }
  }
}

