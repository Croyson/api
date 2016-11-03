package com.wisedu.next.api.controllers

import javax.inject.Inject

import com.twitter.finatra.http.Controller
import com.wisedu.next.api.domains._
import com.wisedu.next.api.filters.UserContext._
import com.wisedu.next.api.services.CircleUserService

/**
 * Version: 1.1
 * Author: pattywgm
 * Time: 16/7/6 上午11:01
 * Desc:
 */
class CircleUserController extends Controller {

  @Inject var circleUserService: CircleUserService = _

  // 用户关注
  post("/v2/circles/user/attention") { request: PostCircleUserRequest =>
    circleUserService.attention(request).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
  }


  // 用户取消关注
  post("/v2/circles/user/attention/cancel") { request: PostCircleUserRequest =>
    circleUserService.cancelAttention(request).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
  }

  //  获取关注列表
  get("/v2/user/:user_id/following") { request: GetFollowingUsersRequest =>
    val userId = request.user_id
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)
    circleUserService.getFollowUsersList(userId, limits, offset, 0).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(GetFollowedUserResponse("success", "", Some(resp)))
    }
  }


  get("/v2/user/focus/state") { request: UserStatRequest =>
    val followId = request.user_id
    val userId = request.request.user.userId
    circleUserService.getUserEachAttentionStat(userId, followId).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(GetUserFocusStateResponse("success", "", resp))
    }
  }

  //  获取粉丝列表
  get("/v2/user/:user_id/followed") { request: GetFollowingUsersRequest =>
    val userId = request.user_id
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)
    circleUserService.getFollowUsersList(userId, limits, offset, 1).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(GetFollowedUserResponse("success", "", Some(resp)))
    }
  }

  //获取用户粉丝数,关注数,动态数
  get("/v2/user/:user_id/stat") { request: GetUserStatRequest =>
    val theUserId = request.request.user.userId
    val userId = request.user_id
    circleUserService.getUserStat(userId, theUserId).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
  }
}


