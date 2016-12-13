package com.wisedu.next.api.services

import java.util.UUID
import javax.inject.{Inject, Singleton}

import com.twitter.util.Future
import com.wisedu.next.api.domains._
import com.wisedu.next.api.filters.UserContext._
import com.wisedu.next.models._
import com.wisedu.next.services._
import org.joda.time.DateTime


@Singleton
class UpdatesService {

  @Inject var updateBaseService: UpdateBaseService = _
  @Inject var feedBaseService: FeedBaseService = _
  @Inject var userBaseService: UserBaseService = _
  @Inject var collegeBaseService: CollegeBaseService = _
  @Inject var emotionService: EmotionService = _
  @Inject var sysCodeBaseService: SysCodeBaseService = _
  @Inject var staticBaseService: StaticBaseService = _
  @Inject var serviceFunctions: ServiceFunctions = _
  @Inject var feedService: FeedsService = _
  @Inject var emotionsService: EmotionsService = _
  @Inject var msgBaseService: MsgBaseService = _
  @Inject var baseService: BaseService = _
  @Inject var updateService: UpdatesService = _
  @Inject var msgService: MsgService = _
  @Inject var circlesBaseService: CirclesBaseService = _

  // 发布评论
  def postUpdates(request: PostUpdatesRequest): Future[PostUpdatesResponse] = {
    val method = request.method
    val is_anonymous = request.is_anonymous
    val feed_id = request.update.feed_id
    val pupdate_id = request.update.update_id.getOrElse("")
    val content = request.update.content
    val img_urls = request.update.img_urls.getOrElse("")
    val update_type = request.update.update_type
    val thresh_hold = request.update.thresh_hold.getOrElse(0)
    feedBaseService.getFeedById(feed_id).flatMap {
      case Some(feed) =>
        val pstImg = if ("6".equals(feed.viewStyle) && "0".equals(method)) {
          //话题的一级评论即原创
          PostMsgRequest(request.request, "1", is_anonymous, PostMsgReq(Some(feed_id.toString), None, content, Some(img_urls),
            update_type, Some(thresh_hold), None, None, None, None, None))
        } else if (!"6".equals(feed.viewStyle) && "0".equals(method)) {
          //非话题的评论 一级评论
          PostMsgRequest(request.request, "1", is_anonymous, PostMsgReq(Some(feed_id.toString), None, content, Some(img_urls),
            update_type, Some(thresh_hold), None, None, None, None, None))
        } else {
          //二级评论
          PostMsgRequest(request.request, "1", is_anonymous, PostMsgReq(None, Some(pupdate_id), content, Some(img_urls),
            update_type, Some(thresh_hold), None, None, None, None, None))
        }
        msgService.postMsg(pstImg).map {
          rsp => if ("success".equals(rsp.status)) {
            PostUpdatesResponse("success", "",
              Some(PostUpdatesResp(feed_id,
                rsp.postMsgResp match {
                  case Some(item) => item.msg_id
                  case None => UUID.randomUUID()
                }
                ,
                content, img_urls)))
          } else {
            PostUpdatesResponse("failed", "", None)
          }
        }
      case None => Future(PostUpdatesResponse("failed", "", None))
    }

  }

  // 评论删除
  def delUpdatesLogicById(updateId: UUID, userId: String): Future[(Boolean, String)] = {
    try {
      msgBaseService.getMsgById(updateId).flatMap {
        case Some(msg) =>
          //获取资讯所在圈子的管理员
          circlesBaseService.getCircleById(msg.groupId).map {
            case Some(circle) => circle.adminUser
            case None => ""
          }.flatMap {
            adminUser => if (msg.cUserId.toString.equals(userId) || adminUser.indexOf(userId) > -1) {
              val delF = if (msg.cUserId.toString.equals(userId)) {
                msgBaseService.delMessageInfo(updateId)
              } else {
                msgBaseService.delMessageInfoByAdmin(updateId)
              }
              delF.flatMap {
                rst =>
                  if ((msg.feedId.isEmpty && msg.updateLevel != 0 && msg.messageType == 1) || (msg.updateLevel == 0 || msg.messageType == 2)) {
                    if (msg.isDelete == 0 && (msg.updateLevel == 0 || msg.messageType == 2)) {
                      //评论转发的消息减少动态数
                      userBaseService.decMsgCount(UUID.fromString(msg.cUserId))
                    }
                    //消息的评论 消息的转发
                    msgBaseService.getReferenceMsg(msg.messageId).flatMap {
                      case Some(reMsg) => msgBaseService.decMsgUpdateNum(reMsg.messageId).map {
                        rst => (true, "删除成功!")
                      }
                      case None => Future(true, "删除成功!")
                    }
                  } else
                  if (msg.feedId.nonEmpty && BaseFunctions.isUUID(msg.feedId)) {
                    //咨询的评论 话题的消息
                    feedBaseService.decFeedUpdateNum(UUID.fromString(msg.feedId)).map {
                      rst => (true, "删除成功!")
                    }
                  } else {
                    Future((true, "删除成功!"))
                  }

              }
            }
            else Future((false, "没有删除权限!"))
          }
        case None => Future((false, "消息不存在!"))
      }

    } catch {
      case _: Exception => Future((false, "系统繁忙,请稍后再试!"))
    }
  }

  // 评论详情
  def getById(updateId: UUID, userId: UUID): Future[FeedUpdateResp] = {

    msgBaseService.getUpdateById(updateId).flatMap {
      case Some(update) => {

        // 评论的创建用户
        val userF = userBaseService.getUserById(update.userId)

        // 评论点赞用户
        val updateLikeUsersF = getUpdateLikeUsers(updateId.toString, 5, 0)
        // emotion 情绪数据
        val respF = emotionsService.getEmotions(updateId.toString, "1", 5, 0)

        // 评论的赞\踩纪录
        val updateLikeF = updateBaseService.getLikeById(userId, updateId, 1)

        // 当前用户是否发布情绪
        val isEmotionF = baseService.isEmotion(userId, updateId.toString, 1)
        for {
          user <- userF
          updateLikeUsers <- updateLikeUsersF
          resp <- respF
          updateLike <- updateLikeF
          isEmotion <- isEmotionF
        } yield (user, updateLikeUsers, resp, updateLike, update, isEmotion)
      }.flatMap {
        case (user, updateLikeUsers, resp, updateLike, update, isEmotion) =>
          val like = updateLike match {
            case Some(item) => "1"
            case None => "0"
          }
          val u = user match {
            case Some(item) => item
            case None => User(update.userId, "不存在用户,逻辑数据有误", "不存在用户,逻辑数据有误",
              " ", " ", 1, " ", " ", " ", " ", " ", " ", " ", "", "", "",
              DateTime.now, "", " ", "", "", "", DateTime.now, "", "", 0, 0, "", "", 1)
          }
          val depNameF = collegeBaseService.getDepartById(u.depart).map {
            case Some(dep) => dep.departName
            case None => ""
          }

          val collegeShortNameF = collegeBaseService.getCollegeById(u.collegeId).map {
            case Some(college) => college.shortName
            case None => ""
          }

          val isRealNamePostF = circlesBaseService.getCircleByMsgId(updateId.toString).flatMap {
            case Some(circle) => Future(circle.isRealNamePost)
            case None => Future(0)
          }

          for {
            depName <- depNameF
            collegeShortName <- collegeShortNameF
            isRealNamePost <- isRealNamePostF
          } yield serviceFunctions.toGetFeedUpdateResponse(u, update, like, isEmotion, resp, updateLikeUsers, u.collegeName, collegeShortName, depName, isRealNamePost)

      }.map {
        item => FeedUpdateResp("success", "", Some(item))
      }
      case None => Future(FeedUpdateResp("failed", "not exist!", None)) //不存在返回失败
    }
  }

  // 获取评论列表
  def getUpdateList(request: GetUpdatesRequest): Future[GetUpdatesResponse] = {
    val update_id = request.update_id.getOrElse("")
    val method = request.method
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)
    val feed_id = request.feed_id
    val user_id = request.request.user.userId

    if (method.equals("0")) {
      feedBaseService.getFeedById(feed_id).flatMap {
        case Some(feed) =>
          msgBaseService.collUpdatesByIdWithUserIds(feed_id.toString, feed.topicInviter, "sortNo asc", limits, offset).flatMap {
            updates => Future.collect(updates.map {
              update => getUpdateInfo(update, user_id, "1").map {
                case (user, updateNum, departName, like, updateLikeNum, resp, updateLikeUsers, collegeShortName, isEmotion, isRealNamePost) =>
                  serviceFunctions.toGetUpdatesResp(feed, update, user, updateNum, departName,
                    like, isEmotion, resp, updateLikeNum, updateLikeUsers, collegeShortName, isRealNamePost,
                    "", "", "", "")
              }
            })
          }.map(item => GetUpdatesResponse("success", "", item))
        case None => Future {
          GetUpdatesResponse("fail", "The feed does not exists", List())
        }
      }
    } else {
      circlesBaseService.getCircleByMsgId(update_id).flatMap {
        case Some(circle) => Future(circle.isRealNamePost)
        case None => Future(0)
      }.flatMap {
        isRealNamePost => msgBaseService.collUpdatesById(update_id, "updateId", limits, offset).flatMap {
          updates => Future.collect(updates.map {
            update => getUpdateInfo(update, user_id, "1").map {
              case (user, updateNum, departName, like, updateLikeNum, resp, updateLikeUsers, collegeShortName, isEmotion, isRealNamePost) =>
                serviceFunctions.toGetUpdatesResp(update, user, updateNum, departName, like, isEmotion, resp, updateLikeNum, updateLikeUsers,
                  collegeShortName, isRealNamePost, "", "", "", "")
            }
          })
        }
      }.map(item => GetUpdatesResponse("success", "", item))
    }
  }

  // 获取最热的一级评论列表
  def getHotUpdateList(request: GetUpdatesTopRequest): Future[GetUpdatesTopResponse] = {
    val limits = request.limits.getOrElse(5)
    val feed_id = request.feed_id
    val user_id = request.request.user.userId

    msgBaseService.getHotUpdatesByFeedId(feed_id, limits).flatMap {
      feedUpdates => Future.collect(feedUpdates.map {
        feedUpdate =>
          val isLikeF = updateBaseService.getLikeById(user_id, feedUpdate.updateId, 1).map {
            case Some(item) => "1"
            case None => "0"
          }

          val userF = userBaseService.getUserById(feedUpdate.userId).map {
            case Some(item) => item
            case None => User(feedUpdate.userId, "", "",
              "", "", 1, "", "", "", "", "", "", "", "", "", "",
              DateTime.now, "", "", "", "", "", DateTime.now, "", "", 0, 0, "", "", 1)
          }
          // 当前用户是否发布情绪
          val isEmotionF = baseService.isEmotion(user_id, feedUpdate.updateId.toString, 1)
          val isRealNamePostF = circlesBaseService.getCircleByMsgId(feedUpdate.updateId.toString).flatMap {
            case Some(circle) => Future(circle.isRealNamePost)
            case None => Future(0)
          }
          for {
            isLike <- isLikeF
            user <- userF
            isEmotion <- isEmotionF
            isRealNamePost <- isRealNamePostF
          } yield serviceFunctions.toGetUpdatesTopResp(feedUpdate, user, isLike, isEmotion, isRealNamePost)
      })
    }.map(items => GetUpdatesTopResponse(items))
  }


  // 根据码表获取评论模式
  def getUpdateMode(request: GetUpdateModeRequest): Future[GetUpdateModeResponse] = {
    val feed_id = request.feed_id
    feedBaseService.getFeedById(UUID.fromString(feed_id)).flatMap {
      case Some(item) =>
        val updateType = if (item.topicInviter.isEmpty) 1 else 3
        sysCodeBaseService.getByTypeId("mode_type").map {
          modes => GetUpdateModeResponse(modes.reverse.map(item => GetUpdateModeResp(item.value.toInt, item.display)), updateType)
        }
      case None => Future(GetUpdateModeResponse(List(GetUpdateModeResp(0, "")), 0))
    }
  }


  //获取评论点赞用户
  def getUpdateLikeUsers(updateId: String, limits: Int, offset: Int): Future[Seq[GetEmotionsUserResp]] = {
    val usersF = circlesBaseService.getCircleByMsgId(updateId).flatMap {
      case Some(circle) => Future(circle.isRealNamePost)
      case None => Future(0)
    }.flatMap {
      isRealNamePost => userBaseService.getUsersByUpdateId(updateId, limits, offset).flatMap {
        users => Future.collect(users.map {
          user =>
            if (user.isAnonymousUser == 1) {
              Future(GetEmotionsUserResp(user.userId.toString, "", "", "", "", "", "", "", "", ""))
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
              } yield GetEmotionsUserResp(user.userId.toString, if (isRealNamePost == 1 && user.name.nonEmpty) user.name else user.alias, user.name, user.freshDate, user.imgUrl, user.sex.toString, user.collegeName, collegeShortName,
                departName._2, departName._1)
            }
        })
      }
    }
    usersF
  }

  // 获取评论详细数据
  def getUpdateInfo(update: FeedUpdate, user_id: UUID, method: String): Future[(User, Long, String, String, Long, Seq[GetEmotionsResp], Seq[GetEmotionsUserResp], String, String, Int)] = {
    val stats = {
      // 当前用户是否点过赞
      val cuserF = updateBaseService.getLikeById(user_id, update.updateId, 1)
      // 当前用户是否发布情绪
      val isEmotionF = baseService.isEmotion(user_id, update.updateId.toString, 1)
      val userF = userBaseService.getUserById(update.userId).map(_.getOrElse(userBaseService.getDefaultUser()))
      // 子评论数
      val updateNum = update.subUpdateNum
      // 评论点赞数
      val updateLikeNum = update.likeNum

      // 评论点赞用户
      val updateLikeUsersF = getUpdateLikeUsers(update.updateId.toString, 5, 0)

      //圈子是否强制实名
      val isRealNamePostF = circlesBaseService.getCircleByMsgId(update.updateId.toString).flatMap {
        case Some(circle) => Future(circle.isRealNamePost)
        case None => Future(0)
      }
      // emotion 情绪数据
      val respF = emotionsService.getEmotions(update.updateId.toString, method, 5, 0)
      //获取回复人

      for {
        cuser <- cuserF
        user <- userF
        resp <- respF
        updateLikeUsers <- updateLikeUsersF
        isEmotion <- isEmotionF
        isRealNamePost <- isRealNamePostF
      } yield (cuser, user, updateNum, updateLikeNum, resp, updateLikeUsers, isEmotion, isRealNamePost)
    }.flatMap {
      case (cuser, user, updateNum, updateLikeNum, resp, updateLikeUsers, isEmotion, isRealNamePost) =>
        val like = cuser match {
          case Some(item) => "1"
          case None => "0"
        }

        val departNameF = collegeBaseService.getDepartById(user.depart).map {
          case Some(dep) => dep.departName
          case None => ""
        }

        val collegeShortNameF = collegeBaseService.getCollegeById(user.collegeId).map {
          case Some(college) => college.shortName
          case None => ""
        }
        for {
          departName <- departNameF
          collegeShortName <- collegeShortNameF
        } yield (user, updateNum, departName, like, updateLikeNum, resp, updateLikeUsers, collegeShortName, isEmotion, isRealNamePost)
    }.map {
      case (user, updateNum, departName, like, updateLikeNum, resp, updateLikeUsers, collegeShortName, isEmotion, isRealNamePost) =>
        (user, updateNum, departName, like, updateLikeNum, resp, updateLikeUsers, collegeShortName, isEmotion, isRealNamePost)
    }
    stats
  }

  // 评论点赞
  def putUpdateLike(request: PutUpdateLikeRequest): Future[Boolean] = {
    try {
      val user_id = request.request.user.userId
      val updateLike = serviceFunctions.toUpdateLike(user_id, request.update_id, request.op_type)
      msgBaseService.getMsgById(request.update_id).map(_.get).flatMap {
        feedUpdate =>
          updateBaseService.getUpdateLikeById(user_id, request.update_id).flatMap {
            case Some(updateL) =>
              updateBaseService.updUpdateLike(updateLike).flatMap {
                result =>
                  if (request.op_type != updateL.opType) {
                    if (request.op_type == 0) {
                      msgBaseService.incMsgUnLikeNum(request.update_id).flatMap {
                        result => msgBaseService.decMsgLikeNum(request.update_id).map {
                          result => true
                        }
                      }
                    }
                    else {
                      msgBaseService.incMsgLikeNum(request.update_id).flatMap {
                        result =>
                          //更新消息人的消息数量
                          if(!user_id.equals(UUID.fromString(feedUpdate.cUserId))){
                            userBaseService.incUserMsgCount(UUID.fromString(feedUpdate.cUserId))
                          }
                          msgBaseService.decMsgUnLikeNum(request.update_id).map {
                          result => true
                        }
                      }
                    }
                  } else {
                    Future {
                      true
                    }
                  }
              }

            case None =>
              updateBaseService.insUpdateLike(updateLike).flatMap {
                result =>
                  if (request.op_type == 0) {
                    msgBaseService.incMsgUnLikeNum(request.update_id).map {
                      result => true
                    }
                  }
                  else {
                    msgBaseService.incMsgLikeNum(request.update_id).map {
                      result =>
                        //更新消息人的消息数量
                        if(!user_id.equals(UUID.fromString(feedUpdate.cUserId))){
                          userBaseService.incUserMsgCount(UUID.fromString(feedUpdate.cUserId))
                        }
                        true
                    }
                  }
              }

          }
      }
    } catch {
      case _: Exception => Future {
        false
      }
    }
  }

  def toGetMsgUpdatesResp(userId: UUID, msg: MessageInfo): Future[GetMsgUpdatesResp] = {

    val img = if (msg.updateType == 3 && msg.likeNum < msg.threshHold) msg.fuzzyImgs else msg.imgUrls
    // 当前用户是否赞过该评论
    val isLikeF = baseService.isLike(userId, msg.messageId)
    // 当前用户是否发布情绪
    val isEmotionF = baseService.isEmotion(userId, msg.messageId.toString, 1)
    // 评论发布者
    val userF = baseService.getUserById(UUID.fromString(msg.cUserId)).flatMap {
      case user =>
        val collegeShortNameF = baseService.getCollegeShortNameByID(user.collegeId)
        val depF = baseService.getDepartShortNameById(user.depart)
        for {
          collegeShortName <- collegeShortNameF
          dep <- depF
        } yield (user, collegeShortName, dep._1, dep._2, img)

    }
    // 评论情绪
    val emotionsF = emotionsService.getEmotions(msg.messageId.toString, "1", 5, 0)
    // 评论点赞者
    val likeUserF = updateService.getUpdateLikeUsers(msg.messageId.toString, 5, 0)
    //圈子是否强制实名
    val isRealNamePostF = circlesBaseService.getCircleByMsgId(msg.messageId.toString).flatMap {
      case Some(circle) => Future(circle.isRealNamePost)
      case None => Future(0)
    }
    //获取评论的回复人
    val replyUserF = if (msg.replyUserId.nonEmpty) {
      userBaseService.getUserById(UUID.fromString(msg.replyUserId)).map {
        case Some(rUser) => (rUser.alias, rUser.name)
        case None => ("", "")
      }
    } else {
      Future("", "")
    }


    for {
      isLike <- isLikeF
      user <- userF
      emotions <- emotionsF
      likeUser <- likeUserF
      isEmotion <- isEmotionF
      isRealNamePost <- isRealNamePostF
      replyUser <- replyUserF
    } yield (isLike, user, emotions, likeUser, img, isEmotion, isRealNamePost, replyUser)
  }.map {
    case (isLike, user, emotions, likeUser, img, isEmotion, isRealNamePost, replyUser) =>
      val userInfo = user._1
      if (msg.isAnonymous == 0)
        GetMsgUpdatesResp(s"/v2/updates/${msg.messageId.toString}", msg.messageId, userInfo.userId.toString, if (isRealNamePost == 1 && userInfo.name.nonEmpty) userInfo.name else userInfo.alias, userInfo.name, userInfo.freshDate, userInfo.sex,
          userInfo.imgUrl, s"/v2/users/${userInfo.userId.toString}", userInfo.collegeName, user._2, user._4, user._3,
          userInfo.cla, msg.isAnonymous, msg.likeNum, msg.content, img, msg.cTime, msg.updateNum, isLike, isEmotion,
          msg.updateType, msg.threshHold, emotions,
          likeUser, msg.replyMsgId, msg.replyUserId, if (isRealNamePost == 1 && replyUser._2.nonEmpty) replyUser._2 else replyUser._1)
      else
        GetMsgUpdatesResp(s"/v2/updates/${msg.messageId.toString}", msg.messageId, "", "匿名用户", "", "", 0,
          "", "", "", "", "", "", "", msg.isAnonymous, msg.likeNum, msg.content, img, msg.cTime,
          msg.updateNum, isLike, isEmotion, msg.updateType, msg.threshHold, emotions,
          likeUser, msg.replyMsgId, msg.replyUserId, if (isRealNamePost == 1 && replyUser._2.nonEmpty) replyUser._2 else replyUser._1)

  }

  // 获取消息评论
  def getMsgUpdates(user_id: UUID, msg_id: String, sortType: Int, limits: Int, offset: Int): Future[Seq[GetMsgUpdatesResp]] = {
    val msgsF = if (sortType == 0) {
      msgBaseService.getMsgS("", "", msg_id, "1", "1", "", "", "cTime asc", limits, offset)
    } else {
      msgBaseService.getMsgS("", "", msg_id, "1", "1", "", "", "", limits, offset).map {
        updates => updates.sortWith(serviceFunctions.comp_messageInfoCTime_asc)
      }
    }
    val resF = msgsF.flatMap {
      updates => Future.collect(updates.map {
        update => {
          toGetMsgUpdatesResp(user_id, update)
        }
      })
    }
    resF
  }
}