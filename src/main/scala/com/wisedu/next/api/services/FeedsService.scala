package com.wisedu.next.api.services

import java.util.UUID
import javax.inject.Inject

import com.google.inject.Singleton
import com.twitter.util.Future
import com.wisedu.next.api.domains._
import com.wisedu.next.api.filters.UserContext._
import com.wisedu.next.models._
import com.wisedu.next.services._
import org.joda.time.DateTime

/**
 * Version: 1.1
 * Author: pattywgm
 * Time: 16/6/28 上午10:43
 * Desc:
 */

@Singleton
class FeedsService {

  @Inject var feedBaseService: FeedBaseService = _
  @Inject var serviceBaseService: ServiceBaseService = _
  @Inject var userBaseService: UserBaseService = _
  @Inject var serviceFunctions: ServiceFunctions = _
  @Inject var baseService: BaseService = _
  @Inject var groupBaseService: GroupBaseService = _
  @Inject var sysCodeBaseService: SysCodeBaseService = _
  @Inject var feedAndStatsBaseService: FeedAndStatsBaseService = _
  @Inject var emotionService: EmotionsService = _
  @Inject var collegeBaseService: CollegeBaseService = _


  // 内容分享
  def feedShare(request: PutFeedShareRequest): Future[Boolean] = {
    val feed_id = request.feed_id
    val user_id = request.request.user.userId
    val share_type = request.share_type
    val share_url = request.share_url
    val src_value = request.src_value
    val feedShare = serviceFunctions.toFeedShare(feed_id, user_id, share_type, share_url, src_value)

    feedBaseService.insFeedShare(feedShare).flatMap {
      result => feedBaseService.getFeedStatsById(feed_id).flatMap {
        case Some(stat) => feedBaseService.incFeedShareNum(feed_id).map {
          result => true
        }
        case None => feedBaseService.insFeedStat(FeedStat(feed_id, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L)).map {
          result => true
        }
      }
    }.rescue {
      case e: Exception => Future(false)
    }
  }

  // 内容点赞
  def feedLike(request: PutFeedLikeRequest): Future[Boolean] = {
    val feed_id = request.feed_id
    val user_id = request.request.user.userId
    val op_type = request.op_type
    val op_msg = request.op_msg.getOrElse("")

    val feedLike = serviceFunctions.toFeedLike(feed_id, user_id, op_type, op_msg)
    try {
      getFeedStatsById(feed_id)
      feedBaseService.getFeedLikeById(feed_id, user_id).flatMap {
        case Some(item) =>
          feedBaseService.updFeedLike(feedLike)
          if (op_type == 1) {
            feedBaseService.incFeedLikeNum(feed_id).flatMap {
              result =>
                feedBaseService.decFeedUnlikeNum(feed_id).map {
                  result => true
                }
            }
          }
          else {
            feedBaseService.incFeedUnlikeNum(feed_id).flatMap {
              result =>
                feedBaseService.decFeedLikeNum(feed_id).map {
                  result => true
                }
            }
          }
        case None =>
          feedBaseService.insFeedLike(feedLike)
          if (op_type == 1) {
            feedBaseService.incFeedLikeNum(feed_id).map {
              result => true
            }
          }
          else {
            feedBaseService.incFeedUnlikeNum(feed_id).map {
              result => true
            }
          }
      }.rescue {
        case e: Exception => Future(false)
      }
    } catch {
      case _: Exception => Future(false)
    }
  }

  //内容收藏
  def feedCollect(request: PutFeedCollectRequest): Future[Boolean] = {
    try {
      val feed_id = request.feed_id
      val user_id = request.request.user.userId
      val feedCollect = serviceFunctions.toFeedCollect(feed_id, user_id)

      getFeedStatsById(feed_id)
      feedBaseService.insFeedCollect(feedCollect).flatMap {
        result => feedBaseService.incFeedCollectNum(feed_id).map {
          result => true
        }
      }.rescue {
        case e: Exception => Future(false)
      }
    } catch {
      case _: Exception => Future(false)
    }
  }

  // 获取资讯详情
  def getFeedInfos(feed: Feed, feed_id: UUID, user_id: UUID, read_type: Int, src_value: String, push_value: String, ip_addr: String): Future[(String, FeedStat, (String, String, String), SysCode, FeedLike, FeedLotteryDraw, Seq[GetInvitersResp], (String, String, String, String), String)] = {
    val resp = {
      // 当前用户是否关注该媒体
      val userServiceF = userBaseService.checkUserLookService(user_id, feed.serId)
      // 更新或插入阅读流水及统计数据
      val feedReadDetail = serviceFunctions.toFeedReadDetail(feed_id, user_id.toString, read_type, src_value, push_value, ip_addr)
      val feedStatF = feedBaseService.getFeedReadStatsById(user_id.toString, feed_id).flatMap {
        case Some(readStat) =>
          val stat = serviceFunctions.toFeedReadStat(readStat)
          feedBaseService.updFeedReadStat(stat).flatMap {
            result => doWithFeedReadDetailsAndStat(feedReadDetail, feed_id)
          }
        case None =>
          val stat = serviceFunctions.toFeedReadStat(feed_id, user_id.toString)
          feedBaseService.insFeedReadStat(stat).flatMap {
            result => doWithFeedReadDetailsAndStat(feedReadDetail, feed_id)
          }
      }
      // 资讯所属组信息
      val groupF = groupBaseService.getGroupById(feed.displayChannel).map {
        case Some(item) => (item.groupName, item.iconUrl, item.description)
        case None => ("", "", "")
      }
      // 评论类型
      val updateTypeF = if (feed.viewStyle == "6") sysCodeBaseService.getSysCodeById("comment_type_new", "1").map(_.getOrElse(SysCode("", "", "", "", 0, ""))) else sysCodeBaseService.getSysCodeById("comment_type_new", "0").map(_.getOrElse(SysCode("", "", "", "", 0, "")))
      // 当前用户是否喜欢该资讯
      val feedLikeF = feedBaseService.getFeedLikeById(feed.feedId, user_id).map(_.getOrElse(FeedLike(feed.feedId, user_id, 0, DateTime.now, " ")))
      // 当前用户是否喜欢该资讯
      val isEmotionF = baseService.isEmotion(user_id, feed.feedId.toString, 0)
      // 若为抽奖话题,获取抽奖设置信息
      val lotteryF = baseService.getFeedLottery(feed_id.toString)
      // 若为话题,获取话题受邀人信息
      val invitersF = baseService.collInviters(feed.topicInviter)
      // 获取资讯发布者信息
      val mediaF = if (feed.srcType == 1) {
        baseService.getUserById(UUID.fromString(feed.serId)).map {
          user => (user.userId.toString, user.alias, user.imgUrl, "")
        }
      } else {
        baseService.getServiceById(feed.serId).map {
          serv => (serv.serId, serv.name, serv.imgUrl, serv.depict)
        }
      }

      for {
        userService <- userServiceF
        feedStat <- feedStatF
        group <- groupF
        updateType <- updateTypeF
        feedLike <- feedLikeF
        lottery <- lotteryF
        inviters <- invitersF
        media <- mediaF
        isEmotion <- isEmotionF
      } yield (userService, feedStat, group, updateType, feedLike, lottery, inviters, media, isEmotion)
    }.map {
      case (userService, feedStat, group, updateType, feedLike, lottery, inviters, media, isEmotion) =>
        (userService, feedStat, group, updateType, feedLike, lottery, inviters, media, isEmotion)
    }
    resp
  }

  // 获取频道(虚拟组)资讯集合
  def getChannelFeeds(channelId: String, userId: String, limits: Int, offset: Int): Future[GetChannelResponse] = {
    groupBaseService.getGroupById(channelId).flatMap {
      case Some(group) =>
        collGroupFeeds(group.groupId, userId, limits, offset)
      case None => Future {
        GetChannelResponse("fail", "The channel or group does not exists!", List())
      }
    }
  }

  //获取频道广告位资讯集合
  def getPosterFeeds(channelId: String, limits: Int, offset: Int): Future[GetChannelResponse] = {
    groupBaseService.getPosterGroupById(channelId, 0).flatMap {
      case Some(group) =>
        collGroupFeeds(group.groupId, "", limits, offset)
      case None => Future {
        GetChannelResponse("fail", "The channel or group does not exists!", List())
      }
    }
  }

  //获取圈子广告位资讯集合
  def getCircleFeeds(circleId: String, limits: Int, offset: Int): Future[GetChannelResponse] = {
    groupBaseService.getPosterGroupById(circleId, 1).flatMap {
      case Some(group) =>
        collGroupFeeds(group.groupId, "", limits, offset)
      case None => Future {
        GetChannelResponse("fail", "The channel or group does not exists!", List())
      }
    }
  }

  //用户反馈
  def userFeedBack(request: PostFeedbackRequest): Future[Boolean] = {
    val userId = request.request.user.userId
    val content = request.feedback.content
    val contact = request.feedback.contact.getOrElse(" ")
    try {
      feedBaseService.insFeedback(Feedback(userId, DateTime.now, content, contact)).map {
        result => true
      }
    }
    catch {
      case _: Exception => Future {
        false
      }
    }
  }

  // 获取组内资讯集合
  def collGroupFeeds(channel_id: String, userId: String, limits: Int, offset: Int): Future[GetChannelResponse] = {
    val feeds = feedAndStatsBaseService.collFeeds(channel_id, "", "1", "", offset, limits, userId).flatMap {
      feedAndStats => Future.collect(feedAndStats.map {
        feed => serviceBaseService.getServiceById(feed.serId).map {
          case Some(service) => serviceFunctions.toGetChannelFeedResp(feed, service.name, service.imgUrl)
          case None => serviceFunctions.toGetChannelFeedResp(feed, " ", " ")
        }
      }).map(item => GetChannelResponse("success", "", item))
    }
    feeds
  }

  // 获取资讯统计数据
  def getFeedStatsById(feedId: UUID): Unit = {
    feedBaseService.getFeedStatsById(feedId).map(item => if (!item.isDefined) feedBaseService.insFeedStat(FeedStat(feedId, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)))
  }

  // 插入资讯阅读流水,并更新资讯阅读数
  def doWithFeedReadDetailsAndStat(feedReadDetail: FeedReadDetail, feed_id: UUID): Future[FeedStat] = {
    feedBaseService.insFeedReadDetails(feedReadDetail).flatMap {
      result => feedBaseService.getFeedStatsById(feed_id).flatMap {
        case Some(feedstat) => feedBaseService.incFeedReadNum(feed_id).map {
          result => FeedStat(feed_id, feedstat.readNum + 1, feedstat.likeNum, feedstat.unLikeNum,
            feedstat.collectNum, feedstat.updateNum, feedstat.shareNum, feedstat.onlineNum,
            feedstat.voteNum, feedstat.imgNum)
        }
        case None => feedBaseService.insFeedStat(FeedStat(feed_id, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)).map {
          result => FeedStat(feed_id, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)
        }
      }
    }
  }

  // 获取资讯点赞用户
  def getFeedLikeUsers(feedId: String, limits: Int, offset: Int): Future[Seq[GetEmotionsUserResp]] = {
    val usersF = userBaseService.getUsersByFeedId(feedId, limits, offset).flatMap {
      users => Future.collect(users.map {
        user =>
          if (user.isAnonymousUser == 1) {
            Future(GetEmotionsUserResp(user.userId.toString, "", "", "", "", "", "", "","",""))
          } else {
            val departNameF = collegeBaseService.getDepartById(user.depart).map {
              case Some(dep) => (dep.departName, dep.departShortName)
              case None => ("", "")
            }
            val collegeShortNameF = collegeBaseService.getCollegeById(user.collegeId).map {
              case Some(college) => college.shortName
              case None => ""
            }
            for {
              departName <- departNameF
              collegeShortName <- collegeShortNameF
            } yield GetEmotionsUserResp(user.userId.toString, user.alias,user.name,user.freshDate, user.imgUrl, user.sex.toString, user.collegeName, collegeShortName,
              departName._2, departName._1)
          }
      })
    }
    usersF
  }
}
