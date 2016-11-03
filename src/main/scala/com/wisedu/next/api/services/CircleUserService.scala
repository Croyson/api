package com.wisedu.next.api.services

import java.util.UUID
import javax.inject.Inject

import com.google.inject.Singleton
import com.twitter.util.Future
import com.wisedu.next.api.domains._
import com.wisedu.next.api.filters.UserContext._
import com.wisedu.next.models.UserRelation
import com.wisedu.next.services.UserBaseService
import org.joda.time.DateTime

/**
 * Version: 1.1
 * Author: croyson
 * Time: 16/7/6 下午3:02
 * Desc:
 */

@Singleton
class CircleUserService {

  @Inject var userBaseService: UserBaseService = _
  @Inject var baseService: BaseService = _

  //关注
  def attention(request: PostCircleUserRequest): Future[PostCirclesResponse] = {
    val userId = request.request.user.userId
    val followId = request.user_id
    if (userId.equals(followId)) {
      Future(PostCirclesResponse("fail", "0003", "自己不能关注自己"))
    } else {
      //查询是否已经关注过次用户
      userBaseService.getRelationById(userId, followId, 0).flatMap {
        case Some(userRelation) => Future(PostCirclesResponse("fail", "0001", "已经关注过此用户"))
        case None =>
          //封装关注信息
          val userR = UserRelation(userId, followId, 0, DateTime.now())
          userBaseService.insUserRelation(userR).flatMap {
            rst =>
              val followF = userBaseService.incFollowCount(userId)
              val fansF = userBaseService.incUserFansCount(followId)
              (for {
                follow <- followF
                fans <- fansF
              } yield (follow, fans)).map {
                case (follow, fans) => PostCirclesResponse("success", "", "")
              }
          }
      }.rescue {
        case e: Exception => Future(PostCirclesResponse("fail", "0002", "系统繁忙,请稍后再试!"))
      }
    }
  }

  //取消关注
  def cancelAttention(request: PostCircleUserRequest): Future[PostCirclesResponse] = {
    val userId = request.request.user.userId
    val followId = request.user_id
    if (userId.equals(followId)) {
      Future(PostCirclesResponse("fail", "0003", "自己不能取消关注自己"))
    } else {
      //查询是否已经关注过次用户
      userBaseService.getRelationById(userId, followId, 0).flatMap {
        case Some(userRelation) =>
          userBaseService.delUserRelationByUserId(userId, followId, 0).flatMap {
            rst =>
              val followF = userBaseService.decFollowCount(userId)
              val fansF = userBaseService.decUserFansCount(followId)
              (for {
                follow <- followF
                fans <- fansF
              } yield (follow, fans)).map {
                case (follow, fans) => PostCirclesResponse("success", "", "")
              }
          }
        case None => Future(PostCirclesResponse("fail", "0001", "没有关注过此用户,或者已经取消"))
      }.rescue {
        case e: Exception => Future(PostCirclesResponse("fail", "0002", "系统繁忙,请稍后再试!"))
      }
    }
  }


  //查询用户是否关注过
  def getUserAttentionStat(userId: UUID, followId: UUID): Future[Int] = {
    if (userId.equals(followId)) {
      Future(-1)
    } else {
      userBaseService.getRelationById(userId, followId, 0).map {
        case Some(userRelation) => 1
        case None => 0
      }
    }

  }

  //查询用户是否互相关注过
  def getUserEachAttentionStat(userId: UUID, followId: UUID): Future[Int] = {
    getUserAttentionStat(userId, followId).flatMap {
      case 1 => getUserAttentionStat(followId, userId).map {
        case 1 => 2
        case _ => 1
      }
      case _ => Future(0)
    }

  }


  def getUserStat(userId: UUID, theUserId: UUID): Future[GetUserStatResponse] = {
    //获取用户关注数
    userBaseService.getUserStatById(userId).flatMap {
      case Some(userStat) => getUserAttentionStat(theUserId, userId).flatMap {
        case item => if (item != 1) {
          Future(GetUserStatResponse("success", "", "", Some(GetUserStatResp(userId.toString, item, 0, userStat.followCount,
            userStat.fansCount, userStat.msgCount))))
        } else {
          getUserAttentionStat(userId, theUserId).map {
            case item2 => if (item2 == 1) {
              GetUserStatResponse("success", "", "", Some(GetUserStatResp(userId.toString, item, 1, userStat.followCount,
                userStat.fansCount, userStat.msgCount)))
            } else {
              GetUserStatResponse("success", "", "", Some(GetUserStatResp(userId.toString, item, 0, userStat.followCount,
                userStat.fansCount, userStat.msgCount)))
            }
          }
        }

      }
      case None => Future(GetUserStatResponse("success", "", "", Some(GetUserStatResp(userId.toString, 0, 0, 0,
        0, 0))))
    }.rescue {
      case e: Exception => Future(GetUserStatResponse("fail", "0002", "系统错误", None))
    }

  }


  // 获取关注列表或粉丝列表
  def getFollowUsersList(userId: UUID, limits: Int, offset: Int, method: Int): Future[Seq[GetFollowedUserResp]] = {
    userBaseService.getUserIds(userId, offset, limits, method).flatMap {
      userIds => Future.collect(userIds.map {
        id => userBaseService.getUserById(UUID.fromString(id))
      }).map(user => user.filter(it => it.isDefined).map(_.get)).flatMap(
        user => Future.collect(user.map {
          user => val collegeShortNameF = baseService.getCollegeShortNameByID(user.collegeId)
            val departNameF = baseService.getDepartShortNameById(user.depart)
            // 是否已关注,主要用于判断是否已关注粉丝
            val isFollowedF = if (method == 1) {
              userBaseService.getRelationById(userId, user.userId, 0).map {
                case Some(item) => 1
                case None => 0
              }
            } else Future {
              1
            }
            (for {
              collegeShortName <- collegeShortNameF
              departName <- departNameF
              isFollowed <- isFollowedF
            } yield (collegeShortName, departName, isFollowed)).map {
              case (collegeShortName, departName, isFollowed) =>
                if (method == 0) {
                  GetFollowedUserResp(user.userId.toString, user.alias, user.name, user.freshDate, user.imgUrl,
                    user.sex.toString, user.collegeName, collegeShortName, departName._2, departName._1, 1)
                } else {
                  GetFollowedUserResp(user.userId.toString, user.alias, user.name, user.freshDate, user.imgUrl,
                    user.sex.toString, user.collegeName, collegeShortName, departName._2, departName._1, isFollowed)
                }
            }
        })
      )
    }
  }


}
