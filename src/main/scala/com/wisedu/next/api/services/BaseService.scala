package com.wisedu.next.api.services

import java.util.UUID
import javax.inject.Inject

import com.google.inject.Singleton
import com.twitter.util.Future
import com.wisedu.next.api.domains.GetInvitersResp
import com.wisedu.next.models.{User, FeedLotteryDraw, FeedStat, Service}
import com.wisedu.next.services._
import org.joda.time.DateTime

/**
  * Version: 1.1
  * Author: pattywgm 
  * Time: 16/6/27 上午9:34
  * Desc:
  */

@Singleton
class BaseService {

  @Inject var serviceBaseService: ServiceBaseService = _
  @Inject var feedBaseService: FeedBaseService = _
  @Inject var userBaseService: UserBaseService = _
  @Inject var collegeBaseService: CollegeBaseService = _
  @Inject var updateBaseService: UpdateBaseService = _
  @Inject var groupBaseService: GroupBaseService = _
  @Inject var emotionService: EmotionService = _


  // 根据ID获取用户
  def getUserById(user_id: UUID): Future[User] = {
    userBaseService.getUserById(user_id).map{
      case Some(item) => item
      case None => User(user_id, "", "",
        "", "", 1, "", "", "", "", "", "", "", "", "", "",
        DateTime.now, "", "", "", "", "", DateTime.now, "", "", 1, 1,"","",1)
    }
  }

  // 根据媒体编号获取媒体信息
  def getServiceById(serId: String): Future[Service] = {
    serviceBaseService.getServiceById(serId).map {
      case Some(service) => service
      case None => Service(serId, "", 0, "", 0, "", "", "", DateTime.now, DateTime.now, "", 0,0)
    }
  }

  // 根据资讯ID获取其统计数据
  def getFeedStatById(feedId: UUID): Future[FeedStat] = {
    feedBaseService.getFeedStatsById(feedId).map {
      case Some(item) => item
      case None => FeedStat(feedId, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)
    }
  }

  // 获取抽奖话题
  def getFeedLottery(feed_id: String): Future[FeedLotteryDraw]={
    feedBaseService.getFeedLotteryByFeedId(feed_id).map {
      case Some(lottery) => lottery
      case None =>
        FeedLotteryDraw(feed_id, "", "", "", "", "", DateTime.now, DateTime.now)
    }
  }

  // 获取受邀人集合
  def collInviters(inviters: String): Future[Seq[GetInvitersResp]] = {
    userBaseService.collUsersByIds(inviters).map {
      users => users.map(item => GetInvitersResp(item.userId, item.imgUrl, s"/v2/users/${item.userId.toString}", item.sex))
    }
  }

  // 根据学校ID获取学校简称
  def getCollegeShortNameByID(college_id: String): Future[String]= {
    val collegeShortNameF = collegeBaseService.getCollegeById(college_id).map {
      case Some(college) => college.shortName
      case None => ""
    }
    collegeShortNameF
  }

  // 当前用户是否点过赞
  def isLike(user_id: UUID, update_id: UUID): Future[String] = {
    updateBaseService.getLikeById(user_id, update_id, 1).map{
      case Some(item) => "1"
      case None => "0"
    }
  }

  /* 当前用户是否发表过情绪
  *  opType 0 话题或者咨询的情绪  id 为feedId
  *         1 消息或者评论的情绪   id 为messageId 即 updateId
  * */
  def isEmotion(user_id: UUID, id: String,opType:Int): Future[String] = {
    emotionService.getByContentUserId( id,user_id.toString, opType).map{
      case Some(item) => item.selectId
      case None => "0"
    }
  }

  // 获取组名称
  def getGroupName(group_id:String): Future[String] = {
    groupBaseService.getGroupById(group_id).map{
      case Some(item) => item.groupName
      case None => ""
    }
  }

  // 根据院系ID获取院系全称和简称
  def getDepartShortNameById(depart_id: String): Future[(String,String)] = {
    val depF = collegeBaseService.getDepartById(depart_id).map {
      case Some(item) => (item.departName, item.departShortName)
      case None => ("", "")
    }
    depF
  }
}
