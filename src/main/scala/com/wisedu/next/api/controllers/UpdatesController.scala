package com.wisedu.next.api.controllers

import javax.inject.Inject

import com.twitter.finatra.http.Controller
import com.wisedu.next.api.domains._
import com.wisedu.next.api.filters.UserContext._
import com.wisedu.next.api.services.UpdatesService

/**
 * Version: 1.1
 * Author: pattywgm
 * Time: 16/5/20 上午9:35
 * Desc:
 */

class UpdatesController extends Controller {

  @Inject var updatesService: UpdatesService = _

  // 发布评论
  post("/v2/feeds/:feed_id/updates") { request: PostUpdatesRequest =>
    updatesService.postUpdates(request).map {
      resp => response.ok.json(resp)
    }
  }

  // 获取评论详情
  get("/v2/updates/:update_id") { request: GetFeedUpdateRequest =>
    val userId = request.request.user.userId
    updatesService.getById(request.update_id, userId).map {
      resp => resp.datas match {
        case Some(item) => response.ok.json(item)
        case None => response.badRequest.json(Map("errMsg" -> "The update does not exist!"))
      }
    }
  }

  // 评论删除
  post("/v2/update/:update_id/delete") { request: UpdatesDelReq =>
    val userId = request.request.user.userId
    val updateId = request.update_id

    updatesService.delUpdatesLogicById(updateId, userId.toString).map {
      rst => if (rst._1)
        response.ok.header("Access-Control-Allow-Origin", "*").json(UpdateOpResp("success", rst._2))
      else
        response.badRequest.header("Access-Control-Allow-Origin", "*").json(UpdateOpResp("failed", rst._2))
    }

  }

  // 获取评论列表
  get("/v2/feeds/:feed_id/updates") { request: GetUpdatesRequest =>
    updatesService.getUpdateList(request).map {
      resp => response.ok.json(resp)
    }
  }

  // 获取最热的一级评论列表
  get("/v2/feeds/:feed_id/updates/top") { request: GetUpdatesTopRequest =>
    updatesService.getHotUpdateList(request).map {
      resp => response.ok.json(resp)
    }
  }

  // 根据码表获取评论模式
  get("/v2/:feed_id/update/mode") { request: GetUpdateModeRequest =>
    updatesService.getUpdateMode(request).map {
      resp => response.ok.json(resp)
    }
  }

  // 评论点赞
  put("/v2/updates/:update_id/like") { request: PutUpdateLikeRequest =>
    updatesService.putUpdateLike(request).map {
      resp =>
        if (true) response.ok.header("Access-Control-Allow-Origin", "*").json(UpdateOpResp("success", "")) else response.ok.json(UpdateOpResp("fail", "update like fail"))
    }
  }

  // 评论点赞
  post("/v2/updates/:update_id/like") { request: PutUpdateLikeRequest =>
    updatesService.putUpdateLike(request).map {
      resp =>
        if (true) response.ok.header("Access-Control-Allow-Origin", "*").json(UpdateOpResp("success", "")) else response.ok.json(UpdateOpResp("fail", "update like fail"))
    }
  }

  // 获取消息评论列表
  get("/v2/msgs/:msg_id/updates") { request: GetMsgUpdatesRequest =>
    val user_id = request.request.user.userId
    val msg_id = request.msg_id
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)
    val sortType = request.sort_type.getOrElse(0)
    updatesService.getMsgUpdates(user_id, msg_id,sortType, limits, offset).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(GetMsgUpdatesResponse("success", "", resp))
    }
  }
}
