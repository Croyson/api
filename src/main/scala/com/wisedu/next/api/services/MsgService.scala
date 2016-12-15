package com.wisedu.next.api.services

import java.util.UUID
import javax.inject.Inject

import com.google.inject.Singleton
import com.twitter.inject.Logging
import com.twitter.util.Future
import com.wisedu.next.api.domains._
import com.wisedu.next.api.filters.UserContext._
import com.wisedu.next.models.{MessageInfo, MsgRelation}
import com.wisedu.next.services._
import org.joda.time.DateTime

/**
 * Version: 1.1
 * Author: pattywgm
 * Time: 16/7/6 下午4:16
 * Desc:
 */

@Singleton
class MsgService extends Logging {

  @Inject var msgBaseService: MsgBaseService = _
  @Inject var userBaseService: UserBaseService = _
  @Inject var baseService: BaseService = _
  @Inject var collegeBaseService: CollegeBaseService = _
  @Inject var emotionsService: EmotionsService = _
  @Inject var emotionService: EmotionService = _
  @Inject var updateService: UpdatesService = _
  @Inject var serviceFunctions: ServiceFunctions = _
  @Inject var feedBaseService: FeedBaseService = _
  @Inject var staticBaseService: StaticBaseService = _
  @Inject var circlesBaseService: CirclesBaseService = _
  @Inject var circleUserService: CircleUserService = _

  //获取消息的情形表达
  def getMsgExpress(request: GetMsgInfoRequest): Future[GetMsgInfoExpressResponse] = {
    msgBaseService.getMsgById(UUID.fromString(request.msg_id)).flatMap {
      case Some(msg) =>
        // 获取情绪数据
        val emotionsF = emotionsService.getEmotions(msg.messageId.toString, "1", 10, 0)
        // 获取点赞用户
        val likeUserF = updateService.getUpdateLikeUsers(msg.messageId.toString, 10, 0)

        for {
          emotions <- emotionsF
          likeUser <- likeUserF
        } yield GetMsgInfoExpressResponse("success", "", Some(GetMsgInfoExpressResp(msg.messageId.toString, msg.likeNum, msg.unLikeNum, emotions,
          likeUser)))
      case None => Future {
        GetMsgInfoExpressResponse("fail", "The message does not exists!", None)
      }
    }
  }

  //获取点赞人列表 或者 情绪表达人列表
  def getMsgExpressList(request: GetMsgExpressListRequest): Future[GetEmotionListResponse] = {
    val expressUsersF = if ("0".equals(request.method)) {
      //喜欢列表
      updateService.getUpdateLikeUsers(request.msg_id, request.limits.getOrElse(10), request.offset.getOrElse(0))
    } else {
      //情绪列表
      circlesBaseService.getCircleByMsgId(request.msg_id).flatMap {
        case Some(circle) => Future(circle.isRealNamePost)
        case None => Future(0)
      }.flatMap {
        isRealNamePost => emotionService.collectUserById(request.model_id.getOrElse("mode1"), request.select_id.getOrElse(""), "1", request.msg_id,
          request.limits.getOrElse(10), request.offset.getOrElse(0)).flatMap {
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
      }
    }
    expressUsersF.map {
      items => GetEmotionListResponse("success", "", Some(items))
    }
  }


  // 获取新鲜事详情
  def getMsgInfo(request: GetMsgInfoRequest): Future[GetMsgInfoResponse] = {
    val userId = request.request.user.userId
    // 阅读数加一
    msgBaseService.incMsgReadNum(UUID.fromString(request.msg_id))
    msgBaseService.getMsgById(UUID.fromString(request.msg_id)).flatMap {
      case Some(msg) => {
        // 消息来源于转发,获取被引用消息
        val referMsgInfoF = if (msg.messageType == 2) getReferMsgInfo(msg.messageId)
        else Future {
          GetReferMsgResp("", "", "", "", "", "", "", "", "")
        }
        // 获取发布者
        val msgPostUserF = circlesBaseService.getCircleByMsgId(msg.messageId.toString).flatMap {
          case Some(circle) => Future(circle.isRealNamePost)
          case None => Future(0)
        }.flatMap {
          isRealNamePost => getMsgPosterInfo(msg.cUserId, msg.isAnonymous, isRealNamePost)
        }
        // 获取情绪数据
        val emotionsF = emotionsService.getEmotions(msg.messageId.toString, "1", 5, 0)
        // 获取点赞用户
        val likeUserF = updateService.getUpdateLikeUsers(msg.messageId.toString, 5, 0)
        // 当前用户是否点过赞
        val isLikeF = baseService.isLike(request.request.user.userId, msg.messageId)
        // 获取组名称
        val circleF = circlesBaseService.getCircleById(msg.groupId).map {
          case Some(circle) => (circle.circleName, circle.iconUrl, circle.adminUser)
          case None => ("", "", "")
        }
        // 当前用户是否发布情绪
        val isEmotionF = baseService.isEmotion(request.request.user.userId, msg.messageId.toString, 1)
        //是否关注
        val isAttentionF = circleUserService.getUserAttentionStat(userId, UUID.fromString(msg.cUserId))

        for {
          referMsgInfo <- referMsgInfoF
          msgPostUser <- msgPostUserF
          emotions <- emotionsF
          likeUser <- likeUserF
          isLike <- isLikeF
          circle <- circleF
          isEmotion <- isEmotionF
          isAttention <- isAttentionF
        } yield (referMsgInfo, msgPostUser, emotions, likeUser, isLike, circle, isEmotion, isAttention)
      }.map {
        case (referMsgInfo, msgPostUser, emotions, likeUser, isLike, circle, isEmotion, isAttention) =>
          serviceFunctions.toGetMsgInfoResponse(msg, referMsgInfo, msgPostUser, emotions,
            likeUser, isLike, isEmotion, circle._1, circle._2, msg.groupId, isAttention, circle._3)
      }
      case None => Future {
        GetMsgInfoResponse("fail", "The message does not exists!", None)
      }
    }
  }


  def getUserMsgList(userId: UUID, theUserId: UUID, limits: Int, offset: Int): Future[Seq[GetCircleMsgsResp]] = {
    val isAnonymous = if (userId.equals(theUserId)) "" else "0"
    val isDelete = if (userId.equals(theUserId)) "0,2" else ""
    msgBaseService.getUserMsgs(theUserId.toString, isDelete, isAnonymous, limits, offset).flatMap {
      msgs =>
        Future.collect(msgs.map {
          msg => toGetCircleMsgsResp(msg, userId, 0)
        })
    }
  }

  //获取指定圈子的消息
  def getCircleMsgList(request: GetCircleMsgsRequest): Future[Seq[GetCircleMsgsResp]] = {
    val circleId = request.group_id
    val userId = request.request.user.userId
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)
    circlesBaseService.getCircleById(circleId).flatMap {
      case Some(circle) => if (circle.strutsType == 0) {
        //查询圈子的所属消息
        getMsgListWithTop(userId, circle.circleId, "", "", "0", "0,2", circle.topMessages, "", limits, offset)
      } else if (circle.strutsType == 1) {
        //查询该学校的全部消息 先查询所有的普通圈子
        circlesBaseService.collCircles(request.college_id.getOrElse(""), "", "0", "0", 20, 0).map {
          circles => circles.map { circle => circle.circleId }
        }.flatMap {
          ids => getMsgListWithTop(userId, ids.mkString(","), "", "", "0", "0,2", circle.topMessages, "", limits, offset)
        }
      } else if (circle.strutsType == 4) {
        circlesBaseService.collCircles(request.college_id.getOrElse(""), "", "0", "0", 20, 0).map {
          circles => circles.map { circle => circle.circleId }
        }.flatMap {
          ids => getMsgListWithTop(userId, ids.mkString(","), "", "", "0", "0,2", circle.topMessages, "1", limits, offset)
        }
      } else {
        Future(Seq())
      }
      case None => Future(Seq())
    }

  }


  //获取指定圈子的顶贴消息
  def getTopMsgList(request: GetTopMsgsRequest): Future[Seq[GetCircleMsgsResp]] = {
    val collegeId = request.college_id.getOrElse(request.request.user.collegeId)
    val userId = request.request.user.userId
    request.group_id match {
      case Some(circleId) => circlesBaseService.getCircleById(circleId).flatMap {
        case Some(circle) => msgBaseService.getMsgByIds(circle.topMessages, "").flatMap {
          msgs => Future.collect(msgs.map {
            msg => toGetCircleMsgsResp(msg, userId, 1)
          })
        }
        case None => Future(Seq())
      }
      case None =>
        circlesBaseService.getCollegeDefaultDisPlayCircle(collegeId).flatMap {
          case Some(circle) => msgBaseService.getMsgByIds(circle.topMessages, "").flatMap {
            msgs => Future.collect(msgs.map {
              msg => toGetCircleMsgsResp(msg, userId, 1)
            })
          }
          case None => Future(Seq())
        }
    }

  }

  // 获取新鲜事列表
  def getMsgList(userId: UUID, circleId: String, feedId: String, msgId: String, updateLevel: String,
                 msgType: String, excludeMessageId: String, isRecommend: String, limits: Int, offset: Int): Future[Seq[GetCircleMsgsResp]] = {
    msgBaseService.getMsgS(circleId, feedId, msgId, updateLevel, msgType, excludeMessageId, isRecommend, "", limits, offset).flatMap {
      msgs => Future.collect(msgs.map {
        msg => toGetCircleMsgsResp(msg, userId, 0)
      })
    }
  }


  def toGetCircleMsgsResp(msg: MessageInfo, userId: UUID, isTop: Int): Future[GetCircleMsgsResp] = {
    // 当前用户是否点过赞
    val isLikeF = baseService.isLike(userId, msg.messageId)

    // 当前用户是否发布情绪
    val isEmotionF = baseService.isEmotion(userId, msg.messageId.toString, 1)
    // 消息来源于转发,获取被引用消息
    val referMsgInfoF = if (msg.messageType == 2) getReferMsgInfo(msg.messageId)
    else Future {
      GetReferMsgResp("", "", "", "", "", "", "", "", "")
    }
    val msgPostUserF = circlesBaseService.getCircleByMsgId(msg.messageId.toString).flatMap {
      case Some(circle) => Future(circle.isRealNamePost)
      case None => Future(0)
    }.flatMap {
      isRealNamePost => getMsgPosterInfo(msg.cUserId, msg.isAnonymous, isRealNamePost)
    }
    // 获取表情统计数
    val emotionsF = emotionsService.getEmotionsStat(msg.messageId.toString, "1")
    // 获取热门评论列表
    val hotUpdatesF = getHotMsgUpdates(msg.messageId.toString)
    // 获取组名称
    val circleF = circlesBaseService.getCircleById(msg.groupId).map {
      case Some(circle) => (circle.circleName, circle.iconUrl)
      case None => ("", "")
    }
    val isAttentionF = circleUserService.getUserAttentionStat(userId, UUID.fromString(msg.cUserId))
    val infoF = for {
      isLike <- isLikeF
      referMsgInfo <- referMsgInfoF
      msgPostUser <- msgPostUserF
      emotions <- emotionsF
      hotUpdates <- hotUpdatesF
      circle <- circleF
      isEmotion <- isEmotionF
      isAttention <- isAttentionF
    } yield (isLike, referMsgInfo, msgPostUser, emotions, hotUpdates, circle, isEmotion, isAttention)
    infoF.map {
      case (isLike, referMsgInfo, msgPostUser, emotions, hotUpdates, circle, isEmotion, isAttention) =>
        val img = if (msg.updateType == 3 && msg.likeNum < msg.threshHold) msg.fuzzyImgs else msg.imgUrls
        GetCircleMsgsResp(msg.messageId.toString, s"/v2/msgs/${msg.messageId.toString}", msg.content,
          circle._1, msg.groupId, circle._2, img.split(",").filter(_.trim.nonEmpty).mkString(","), msg.messageType.toString, msg.cTime, msg.updateNum, msg.likeNum, isLike, isEmotion, msg.isAnonymous,
          msg.updateType, msg.threshHold, referMsgInfo, emotions, msgPostUser, hotUpdates, isTop, isAttention, msg.isRecommend, msg.isDelete,
          msg.linkTitle, msg.linkImg, msg.linkUrl)
    }
  }

  // 获取学校默认展现圈子的新鲜事
  def getDefaultCircleMsgs(collegeId: String, userId: UUID, isRecommend: String, limits: Int, offset: Int): Future[Seq[GetCircleMsgsResp]] = {
    val circleF =
      if (("1").equals(isRecommend))
        circlesBaseService.getRecommendCircle(collegeId)
      else
        circlesBaseService.getCollegeDefaultDisPlayCircle(collegeId)
    //查询学校的默认展现圈
    circleF.flatMap {
      case Some(circle) =>
        if (circle.strutsType == 0) {
          //查询圈子的所属消息
          getMsgListWithTop(userId, circle.circleId, "", "", "0", "0,2", circle.topMessages, "", limits, offset)
        } else if (circle.strutsType == 1) {
          //查询该学校的全部消息 先查询所有的普通圈子
          circlesBaseService.collCircles(collegeId, "", "0", "0", 20, 0).map {
            circles => circles.map { circle => circle.circleId }
          }.flatMap {
            ids =>
              getMsgListWithTop(userId, ids.mkString(","), "", "", "0", "0,2", circle.topMessages, "", limits, offset)
          }
        } else if (circle.strutsType == 4) {
          circlesBaseService.collCircles(collegeId, "", "0", "0", 20, 0).map {
            circles => circles.map { circle => circle.circleId }
          }.flatMap {
            ids => getMsgListWithTop(userId, ids.mkString(","), "", "", "0", "0,2", circle.topMessages, "1", limits, offset)
          }
        } else {
          Future(Seq())
        }
      case None => Future(Seq())
    }
  }

  def getMsgListWithTop(userId: UUID, circleId: String, feedId: String, msgId: String, updateLevel: String,
                        msgType: String, topMessages: String, isRecommend: String, limits: Int, offset: Int): Future[Seq[GetCircleMsgsResp]] = {
    if (offset == 0) {
      //查询置顶帖
      val topGetCircleMsgsRespF = if (topMessages.nonEmpty)
        msgBaseService.getMsgByIds(topMessages, "").flatMap {
          msgs => Future.collect(msgs.map {
            msg => toGetCircleMsgsResp(msg, userId, 1)
          })
        }
      else {
        Future(Seq())
      }
      //查询第一页的排除置顶帖的5条数据
      val getCircleMsgsRespF = getMsgList(userId, circleId, feedId, msgId, updateLevel, msgType, topMessages, isRecommend, limits, offset)
      val rspF = for {
        topGetCircleMsgsResp <- topGetCircleMsgsRespF
        getCircleMsgsResp <- getCircleMsgsRespF
      } yield (topGetCircleMsgsResp, getCircleMsgsResp)
      rspF.map {
        case (topGetCircleMsgsResp, getCircleMsgsResp) => topGetCircleMsgsResp ++ getCircleMsgsResp
      }
    } else {
      getMsgList(userId, circleId, feedId, msgId, updateLevel, msgType, topMessages, isRecommend, limits, offset)
    }
  }


  // 获取被引用消息详情
  def getReferMsgInfo(msgId: UUID): Future[GetReferMsgResp] = {
    circlesBaseService.getCircleByMsgId(msgId.toString).flatMap {
      case Some(circle) => Future(circle.isRealNamePost)
      case None => Future(0)
    }.flatMap {
      isRealNamePost => msgBaseService.getReferenceMsg(msgId).flatMap {
        case Some(msg) => userBaseService.getUserById(UUID.fromString(msg.cUserId)).map {
          case Some(user) => GetReferMsgResp(msg.messageId.toString, s"/v2/msgs/${msg.messageId.toString}", msg.content,
            msg.imgUrls, user.userId.toString, s"/v2/users/${user.userId.toString}", if (isRealNamePost == 1 && user.name.nonEmpty) user.name else user.alias, user.name, user.freshDate)
          case None => GetReferMsgResp(msg.messageId.toString, s"/v2/msgs/${msg.messageId.toString}", msg.content,
            msg.imgUrls, "", "", "", "", "")
        }
        case None => Future {
          GetReferMsgResp("", "", "", "", "", "", "", "", "")
        }
      }
    }
  }

  // 获取消息发布者信息
  def getMsgPosterInfo(userId: String, isAnonymous: Int, isRealNamePost: Int): Future[GetMsgPosterResp] = {
    userBaseService.getUserById(UUID.fromString(userId)).flatMap {
      case Some(user) => {
        val collegeShortNameF = baseService.getCollegeShortNameByID(user.collegeId)
        val depF = baseService.getDepartShortNameById(user.depart)
        for {
          collegeShortName <- collegeShortNameF
          dep <- depF
        } yield (collegeShortName, dep)
      }.map {
        case (collegeShortName, dep) =>
          // 匿名发布不显示用户相关信息
          if (isAnonymous == 0)
            GetMsgPosterResp(user.userId.toString, s"/v2/users/${user.userId.toString}", if (isRealNamePost == 1 && user.name.nonEmpty && user.name.nonEmpty) user.name else user.alias, user.name, user.freshDate, user.imgUrl,
              user.collegeName, dep._1, user.sex, user.isAnonymousUser, collegeShortName, dep._2)
          else
            GetMsgPosterResp("", "", "匿名用户", "", "", "", "", "", 0, user.isAnonymousUser, "", "")
      }
      case None =>
        Future {
          GetMsgPosterResp("", "", "", "", "", "", "", "", 0, 1, "", "")
        }
    }
  }

  def toGetMsgHotUpdatesResp(msg: MessageInfo, isRealNamePost: Int): Future[GetMsgHotUpdatesResp] = {
    userBaseService.getUserById(UUID.fromString(msg.cUserId)).map {
      case Some(user) =>
        // 评论匿名发布不显示用户信息
        if (msg.isAnonymous == 0)
          (user.userId.toString, s"/v2/users/${user.userId.toString}", user.alias, user.name, user.freshDate)
        else
          ("", "", "匿名用户", "", "")
      case None => ("", "", "", "", "")
    }.flatMap {
      case (userId, userInfo, userAlias, name, freshDate) =>
        var img = msg.imgUrls
        if (msg.updateType == 3) {
          img = if (msg.likeNum >= msg.threshHold) msg.imgUrls else msg.fuzzyImgs
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
        replyUserF.map {
          case (rUserAlias, rUserName) => GetMsgHotUpdatesResp(msg.messageId.toString,
            msg.content, img, userId, userInfo, msg.isAnonymous,
            if (isRealNamePost == 1 && name.nonEmpty) name else userAlias, name, freshDate,
            msg.updateType, msg.threshHold, msg.replyMsgId, msg.replyUserId,
            if (isRealNamePost == 1 && rUserName.nonEmpty) rUserName else rUserAlias,
            msg.linkTitle, msg.linkImg, msg.linkUrl)
        }

    }
  }


  // 获取热门评论信息
  def getHotMsgUpdates(msgId: String): Future[Seq[GetMsgHotUpdatesResp]] = {
    val resF = circlesBaseService.getCircleByMsgId(msgId).flatMap {
      case Some(circle) => Future(circle.isRealNamePost)
      case None => Future(0)
    }.flatMap {
      isRealNamePost => msgBaseService.getMsgS("", "", msgId, "1", "1", "", "", "cTime asc", 5, 0).flatMap {
        updates => Future.collect(updates.map {
          update =>
            toGetMsgHotUpdatesResp(update, isRealNamePost)
        })
      }
    }
    resF
  }

  def postMsg(request: PostMsgRequest): Future[PostMsgResponse] = {
    if (request.msg.content.replaceAll("</?[^>]+>", "").nonEmpty)
    //封装消息
      toMessage(request).flatMap {
        msg => msgBaseService.insMessageInfo(msg).flatMap {
          //插入消息主体
          rst => if (msg.messageType == 0) {
            //原创
            //增加用户发布文章数
            userBaseService.incMsgCount(UUID.fromString(msg.cUserId)).map {
              rst => (true, "")
            }
          } else if (msg.messageType == 2) {
            //转发
            val pMsgId = UUID.fromString(request.msg.p_msg_id.
              getOrElse("09aecfd5-d0ed-448c-8ca9-30a4d7096ccb"))
            //封装关系信息
            msgBaseService.getMsgById(pMsgId).map {
              case Some(pMsg) =>
                //更新主体消息的转发数量
                msgBaseService.incMsgForwardNum(pMsgId)
                //封裝消息关系
                val msgR = MsgRelation(msg.cUserId, msg.messageId.toString, pMsg.cUserId, pMsg.messageId.toString, 2, DateTime.now,
                  (msg.forwardNumD % 10).toInt)

                msgBaseService.insMsgRelation(msgR)
                (true, "")
              case None => (false, "转发消息不存在")
            }

          } else {
            //评论
            val pMsgId = UUID.fromString(request.msg.p_msg_id.
              getOrElse("09aecfd5-d0ed-448c-8ca9-30a4d7096ccb"))

            //如果是 咨询的评论 则更新咨询的信息
            if (msg.feedId.nonEmpty) {
              feedBaseService.incFeedUpdateNum(UUID.fromString(msg.feedId)).map {
                rst => (true, "")
              }
            } else {
              //如果不是咨询的就是消息的评论
              //封装关系信息
              msgBaseService.getMsgById(pMsgId).map {
                case Some(pMsg) =>
                  //更新主体消息的转发数量
                  msgBaseService.incMsgUpdateNum(pMsgId)
                  //增加主体消息人的消息数量
                  if (!request.request.user.userId.equals(pMsg.cUserId)) {
                    userBaseService.incUserMsgCount(UUID.fromString(pMsg.cUserId))
                  }
                  //增加回复消息主体人的消息数量
                  if (msg.replyUserId.nonEmpty && !request.request.user.userId.equals(msg.replyUserId)) {
                    userBaseService.incUserMsgCount(UUID.fromString(msg.replyUserId))
                  }

                  //封裝消息关系
                  val msgR = MsgRelation(msg.cUserId, msg.messageId.toString, pMsg.cUserId, pMsg.messageId.toString, 1, DateTime.now,
                    (msg.updateNumD / 10).toInt)

                  msgBaseService.insMsgRelation(msgR)
                  (true, "")
                case None => (false, "评论主体消息不存在")
              }

            }
          }

        }.flatMap {
          case (rst, errMsg) => if (rst) {
            updateService.toGetMsgUpdatesResp(request.request.user.userId, msg).map {
              item => PostMsgResponse("success", errMsg, Some(item))
            }
          } else {
            Future(PostMsgResponse("failed", errMsg, None))
          }
        }
      }.rescue {
        case e: Exception => Future(PostMsgResponse("failed", "系统繁忙,请稍后再试!", None))
      }
    else
      Future(PostMsgResponse("fail", "去除html标签你的内容为空哟", None))

  }

  //封装消息
  def toMessage(request: PostMsgRequest): Future[MessageInfo] = {
    val feedId = request.msg.feed_id.getOrElse("")

    val messageType = request.method.toInt

    val cUserId = request.request.user.userId

    val updateType = request.msg.update_type

    val threshHold = request.msg.thresh_hold.getOrElse(0)

    val isAnonymous = request.is_anonymous

    val imgUrls = request.msg.msg_img.getOrElse("").split(",").filter(_.trim.nonEmpty).mkString(",")

    // 图片模糊处理,当评论模式为点赞公开时
    val fuzzyImgsF = if (updateType == 3 && imgUrls.nonEmpty) staticBaseService.fuzzyUpdateImgs(imgUrls).map {
      imgs => imgs.mkString(",")
    } else Future {
      imgUrls
    }

    // 图片压缩处理
    val mImgs = if (imgUrls.nonEmpty) {
      imgUrls.split(",").map {
        imgUrl => imgUrl.substring(0, if (imgUrl.lastIndexOf(".") > 0) imgUrl.lastIndexOf(".") else imgUrl.length) + "_m" +
          imgUrl.substring(if (imgUrl.lastIndexOf(".") > 0) imgUrl.lastIndexOf(".") else imgUrl.length, imgUrl.length)
      }.mkString(",")
    } else imgUrls

    val replyMsgF = request.msg.reply_msg_id match {
      case Some(replyMsgId) => msgBaseService.getMsgById(replyMsgId).map {
        case Some(msgItem) => (msgItem.messageId, msgItem.cUserId)
        case None => ("", "")
      }
      case None => Future("", "")
    }

    val updateLevel = if (messageType != 1) {
      //原创或者转发 level 0
      0
    } else {
      //评论的时候为1
      1
    }

    //如果沒有传的情况下获取默认的写入圈子
    val groupIdF = request.msg.group_id match {
      case Some(item) => Future(item)
      case None => circlesBaseService.getCollegeDefaultSaveCircle(request.request.user.collegeId).map {
        case Some(circle) => circle.circleId
        case None => ""
      }
    }

    val content = request.msg.content.replaceAll("</?[^>]+>", "")
    val linkTitle = request.msg.linkTitle.getOrElse("")
    val linkImg = request.msg.linkImg.getOrElse("")
    val linkUrl = request.msg.linkUrl.getOrElse("")
    for {
      fuzzyImgs <- fuzzyImgsF
      groupId <- groupIdF
      replyMsg <- replyMsgF
    } yield MessageInfo(UUID.randomUUID(), feedId, messageType, content, DateTime.now, cUserId.toString, 0, 0, 0, 0, 0, 0, 0, 0, updateType,
      threshHold, isAnonymous, imgUrls, fuzzyImgs, updateLevel, 0, groupId, mImgs, 0, 0, replyMsg._1.toString, replyMsg._2,
      linkTitle, linkImg, linkUrl)

  }


  //查询关注人的新鲜事列表
  def getUserAttentionMsg(userId: UUID, limits: Int, offset: Int): Future[Seq[GetCircleMsgsResp]] = {
    msgBaseService.getUserAttentionMsg(userId.toString, limits, offset).flatMap {
      msgs => Future.collect(msgs.map {
        msg => toGetCircleMsgsResp(msg, userId, 0)
      })
    }
  }


  //推荐\取消推荐消息
  def recommendMsg(request: PostRecommendMsgRequest): Future[PostRecommendMsgResponse] = {
    val userId = request.request.user.userId
    val msgId = request.msg_id
    // 1推荐 0 取消推荐
    val status = request.status
    msgBaseService.getMsgById(msgId).flatMap {
      case Some(msg) => circlesBaseService.getCircleById(msg.groupId).map {
        case Some(circle) => circle.adminUser
        case None => ""
      }.flatMap {
        adminUserId => if (adminUserId.indexOf(userId.toString) == -1) {
          Future(PostRecommendMsgResponse("fail", "0002", "没有操作权限"))
        } else {
          if (status == msg.isRecommend) {
            Future(PostRecommendMsgResponse("fail", "0003", "数据状态已经改变"))
          } else {
            msgBaseService.updateMsgRecommend(msgId.toString, status).map {
              rst => PostRecommendMsgResponse("success", "", "操作成功")
            }
          }
        }
      }
      case None => Future(PostRecommendMsgResponse("fail", "0001", "数据不存在"))
    }.rescue {
      case e: Exception =>
        error("recommendMsg user." + userId.toString + ",err." + e.toString)
        Future(PostRecommendMsgResponse("fail", "0004", "系统繁忙,请稍后再试!"))
    }

  }

  //获取用户消息数量
  def getUseUserMsgCount(userId: UUID): Future[GetUserMsgCountResp] = {
    //验证用户合法性
    userBaseService.getUserById(userId).flatMap {
      case Some(user) => userBaseService.getUserMsgCount(user.userId).map {
        msgCount => GetUserMsgCountResp("success", "", "", Some(msgCount))
      }
      case None => Future(GetUserMsgCountResp("fail", "0001", "用户不存在", None))
    }.rescue {
      case e: Exception =>
        error("getUseUserMsgCount user." + userId.toString + ",err." + e.toString)
        Future(GetUserMsgCountResp("fail", "0004", "系统繁忙,请稍后再试!", None))
    }
  }

  //获取用户消息列表
  def getUserNoticeList(userId: UUID): Future[GetUserNoticeListResp] = {
    //验证用户合法性
    userBaseService.getUserById(userId).flatMap {
      case Some(user) => {
        //获取用户上次登录时间
        userBaseService.getUserMsgListLastTime(user.userId).flatMap {
          time =>
            //重置用戶消息数量
            userBaseService.resetUserMsgCount(userId)
            //重置用户进入列表时间
            userBaseService.resetUserMsgListLastTime(userId)

            (for {
              newNotices <- msgBaseService.getUserNotice(userId.toString, time)
              allNotices <- msgBaseService.getUserNotice(userId.toString, time.plusMonths(-6))
            } yield (newNotices, allNotices)).flatMap {
              case (newNotices, allNotices) => Future.collect(allNotices.sortWith(serviceFunctions.comp_UserMsgNoticeOption_desc).map {
                notice =>
                  circlesBaseService.getCircleByMsgId(notice.msgId.toString).flatMap {
                    case Some(circle) => Future(circle.isRealNamePost)
                    case None => Future(0)
                  }.flatMap {
                    isRealNamePost => getMsgPosterInfo(notice.opUserId, 0, isRealNamePost)
                  }.map {
                    opUser =>
                      val isNew = if (newNotices.contains(notice))
                        1
                      else
                        0
                      UserMsgNoticeResp(notice.msgId, notice.cUserId, notice.cTime, notice.content,
                        notice.imgUrls, notice.circleId, opUser, notice.opTime, notice.opMsgId,
                        notice.opContent, notice.opImgUrls, notice.opType, notice.mainMsgId, isNew)
                  }
              }).map {
                userMsgNotices =>
                  GetUserNoticeListResp("success", "", "", userMsgNotices)
              }
            }
        }
      }
      case None => Future(GetUserNoticeListResp("fail", "0001", "用户不存在", Seq()))
    }.rescue {
      case e: Exception =>
        error("getUserNoticeList user." + userId.toString + ",err." + e.toString)
        Future(GetUserNoticeListResp("fail", "0004", "系统繁忙,请稍后再试!", Seq()))
    }
  }

}


