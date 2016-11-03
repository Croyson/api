package com.wisedu.next.api.services

import java.util.UUID
import javax.inject.{Inject, Singleton}

import com.twitter.util.Future
import com.wisedu.next.api.domains._
import com.wisedu.next.api.filters.UserContext._
import com.wisedu.next.models.{Emotion, EmotionCommunicate}
import com.wisedu.next.services._
import org.joda.time.DateTime

/**
 * Version: 1.1
 * Author: pattywgm
 * Time: 16/6/28 下午1:44
 * Desc:
 */

@Singleton
class EmotionsService {

  @Inject var emotionService: EmotionService = _
  @Inject var updatesService: UpdatesService = _
  @Inject var feedService: FeedsService = _
  @Inject var collegeBaseService: CollegeBaseService = _
  @Inject var circlesBaseService: CirclesBaseService = _
  @Inject var  userBaseService: UserBaseService = _
  @Inject var msgBaseService: MsgBaseService = _
  // 表达情绪
  def postEmotion(request: PostEmotionsRequest): Future[PostEmotionsResponse] = {
    val feedId = request.feed_id.getOrElse("")
    val updateId = request.update_id.getOrElse("")
    val selectId = request.select_id
    val userId = request.request.user.userId

    emotionService.getByUpdateIdFeedIdUserId(updateId, feedId.toString, userId.toString).flatMap {
      case Some(item) =>
        Future {
          PostEmotionsResponse("fail", "fail", "The user has posted one emotion!")
        }
      case None =>
        emotionService.insEmotionCommunicate(EmotionCommunicate(UUID.randomUUID().toString, feedId, updateId, "mode1",
          userId.toString, selectId, DateTime.now)).map {
          result =>
            if(feedId.isEmpty && updateId.nonEmpty){
              //增加用户消息数量
              msgBaseService.getMsgById(UUID.fromString(updateId)).map{
               case Some(msg) =>
                  if(!msg.cUserId.equals(userId)){
                    userBaseService.incUserMsgCount(UUID.fromString(msg.cUserId))
                  }
               case None =>
              }
            }
            PostEmotionsResponse("success", "", "")
        }
    }
  }

  //获取资讯情绪和点赞统计数据
  def getEmotions(request: GetEmotions): Future[GetEmotionsResponse] = {
    val feed_id = request.feed_id.getOrElse("")
    val update_id = request.update_id.getOrElse("")
    val method = request.method

    // 获取情绪统计数
    val emotionF = if (method == "0") getEmotions(feed_id, method, 5, 0) else getEmotions(update_id, method, 5, 0)
    //获取点赞统计数
    val userLikeF = if (method == "0") feedService.getFeedLikeUsers(feed_id, 5, 0) else updatesService.getUpdateLikeUsers(update_id, 5, 0)

    for {
      emotion <- emotionF
      userLike <- userLikeF
    } yield (GetEmotionsResponse("success", "", emotion, userLike, userLike.length))
  }

  // 获取资讯或评论的情绪数据
  def getEmotions(id: String, method: String, limits: Int, offset: Int): Future[Seq[GetEmotionsResp]] = {

    val respF = circlesBaseService.getCircleByMsgId(id).flatMap {
      case Some(circle) => Future(circle.isRealNamePost)
      case None => Future(0)
    }.flatMap{
      isRealNamePost => emotionService.collEmotionSize(id.toString, method, "mode1").flatMap {
        emotionStats => Future.collect(emotionStats.map {
          emotionStat => {
            val emotionF = emotionService.getByModeIdAndSelectId(emotionStat.modelId, emotionStat.selectId).map {
              case Some(emotion) => emotion
              case None => Emotion("", "", "", "", "")
            }
            val usersF = emotionService.collectUserById(emotionStat.modelId, emotionStat.selectId, method, id, limits, offset).flatMap {
              users => Future.collect(users.map {
                user => val departNameF = collegeBaseService.getDepartById(user.depart).map {
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
                  } yield GetEmotionsUserResp(user.userId.toString, if (isRealNamePost == 1 && user.name.nonEmpty) user.name else user.alias, user.name, user.freshDate, user.imgUrl, user.sex.toString, user.collegeName, collegeShortName,
                    departName._2, departName._1)

              })
            }
            for {
              emotion <- emotionF
              user <- usersF
            } yield GetEmotionsResp(emotion.modelId, emotion.selectId, emotion.selectImg, user, emotionStat.count)
          }
        })
      }
    }
        respF

  }
    // 获取表情统计数
    def getEmotionsStat(id: String, method: String): Future[Seq[GetMsgEmotionsResp]] = {
      val respF = emotionService.collEmotionSize(id.toString, method, "mode1").flatMap {
        emotionStats => Future.collect(emotionStats.map {
          emotionStat =>
            emotionService.getByModeIdAndSelectId(emotionStat.modelId, emotionStat.selectId).map {
              case Some(emotion) => GetMsgEmotionsResp(emotion.modelId, emotion.selectId, emotion.selectImg, emotionStat.count)
              case None => GetMsgEmotionsResp("", "", "", emotionStat.count)
            }
        })
      }
      respF
    }

  }