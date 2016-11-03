package com.wisedu.next.api.controllers

import javax.inject.Inject

import com.twitter.finatra.http.Controller
import com.wisedu.next.api.domains._
import com.wisedu.next.api.filters.UserContext._
import com.wisedu.next.api.services.MsgService

/**
  * Version: 1.1
  * Author: pattywgm
  * Time: 16/7/6 上午9:19
  * Desc:
  */
class MsgController extends Controller {

  @Inject var msgService: MsgService = _

  // 发布新鲜事
  post("/v2/msgs"){request: PostMsgRequest =>
    msgService.postMsg(request).map{
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
  }

  //获取新鲜事的情绪表达
  get("/v2/msgs/:msg_id/express") { request: GetMsgInfoRequest =>
    msgService.getMsgExpress(request).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
  }

  //获取新鲜事的情绪表达
  get("/v2/msgs/:msg_id/express/list") { request: GetMsgExpressListRequest =>
    msgService.getMsgExpressList(request).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
  }
  // 获取新鲜事详情
  get("/v2/msgs/:msg_id"){request: GetMsgInfoRequest =>
    msgService.getMsgInfo(request).map{
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
//    GetMsgInfoResponse("success","",Some(GetMsgInfoResp("06939fcf-431b-11e6-83e7-acbc327c3dc9", "活久见,女子背男友过河!","/v2/static/imgs/4100679.jpg",
//      DateTime.now, "杂七圈", 100L, 21L, "2", "1",
//      GetReferMsgResp("80c64a70-432b-11e6-964f-acbc327c3dc9","/v2/msgs/80c64a70-432b-11e6-964f-acbc327c3dc9","被引用消息","/v2/statics/imgs/65756767.jpg","01aedc33-435a-11e6-a851-acbc327c3dc9","/v2/user/01aedc33-435a-11e6-a851-acbc327c3dc9","被引用消息发布者"),
//      GetMsgPosterResp("5d8494c7-431d-11e6-813a-acbc327c3dc9","/v2/statics/user/5d8494c7-431d-11e6-813a-acbc327c3dc9","哈利摩","","南京大学","计算机学院",1,0,"南大","计算机"),
//      List(GetEmotionsResp("mode1","amazed","/v2/static/imgs/emoj2", List(GetEmotionsUserResp("userid1","/v2/imgs/user_icon")), 1)),
//      List(GetEmotionsUserResp("userid2","/v2/imgs/user_icon2")), 1, 0, 0)))
  }

  // 获取指定圈子新鲜事列表
  get("/v2/:group_id/msgs"){request: GetCircleMsgsRequest =>
    msgService.getCircleMsgList(request).map{
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(GetCircleMsgsResponse("success","",resp))
    }
//
//    GetCircleMsgsResponse("success","",List(GetCircleMsgsResp("06939fcf-431b-11e6-83e7-acbc327c3dc9","/v2/msgs/06939fcf-431b-11e6-83e7-acbc327c3dc9","天降红包抢不抢!?!","闺蜜圈","/v2/statics/imgs/411223111.jpg",
//      "2",DateTime.now,109L,208L,"1", 0, 1, 0,
//      GetReferMsgResp("80c64a70-432b-11e6-964f-acbc327c3dc9","/v2/msgs/80c64a70-432b-11e6-964f-acbc327c3dc9","被引用消息","/v2/statics/imgs/65756767.jpg", "01aedc33-435a-11e6-a851-acbc327c3dc9","/v2/user/01aedc33-435a-11e6-a851-acbc327c3dc9","被引用消息发布者"),
//      List(GetMsgEmotionsResp("mode1","anger","/v2/statics/imgs/emoji1",11)),
//      GetMsgPosterResp("5d8494c7-431d-11e6-813a-acbc327c3dc9","/v2/statics/user/5d8494c7-431d-11e6-813a-acbc327c3dc9","哈利摩","","南京大学","计算机学院",1,0,"南大","计算机"),
//      List(GetMsgHotUpdatesResp("udateid1","不抢,奇了怪了...","","9865f257-4324-11e6-9506-acbc327c3dc9","/v2/statics/user/9865f257-4324-11e6-9506-acbc327c3dc9",0,"奇葩哈",0,0)))))
  }

  get("/v2/msgs/top") { request: GetTopMsgsRequest =>
    msgService.getTopMsgList(request).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(GetCircleMsgsResponse("success", "", resp))
    }

  }

  // 获取当前学校默认圈子新鲜事列表
  get("/v2/msgs"){request: GetCollegeMsgsRequest =>
    val collegeId = request.college_id.getOrElse(request.request.user.collegeId)
    val userId = request.request.user.userId
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)
    val isRecommend = request.is_recommend.getOrElse("")
    msgService.getDefaultCircleMsgs(collegeId, userId,isRecommend, limits, offset).map{
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(GetAllCircleMsgsResponse("success","",resp,isRecommend))
    }

//    GetCircleMsgsResponse("success","", List(GetCircleMsgsResp("06939fcf-431b-11e6-83e7-acbc327c3dc9","/v2/msgs/06939fcf-431b-11e6-83e7-acbc327c3dc9","天降红包抢不抢!?!","闺蜜圈","/v2/statics/imgs/411223111.jpg",
//      "2",DateTime.now,109L,208L,"1", 0, 1, 0,
//      GetReferMsgResp("80c64a70-432b-11e6-964f-acbc327c3dc9","/v2/msgs/80c64a70-432b-11e6-964f-acbc327c3dc9","被引用消息","/v2/statics/imgs/65756767.jpg", "01aedc33-435a-11e6-a851-acbc327c3dc9","/v2/user/01aedc33-435a-11e6-a851-acbc327c3dc9","被引用消息发布者"),
//      List(GetMsgEmotionsResp("mode1","anger","/v2/statics/imgs/emoji1",11)),
//      GetMsgPosterResp("5d8494c7-431d-11e6-813a-acbc327c3dc9","/v2/statics/user/5d8494c7-431d-11e6-813a-acbc327c3dc9","哈利摩","","南京大学","计算机学院",1,0,"南大","计算机"),
//      List(GetMsgHotUpdatesResp("udateid1","不抢,奇了怪了...","","9865f257-4324-11e6-9506-acbc327c3dc9","/v2/statics/user/9865f257-4324-11e6-9506-acbc327c3dc9",0,"奇葩哈",0,0)))))
  }

  get("/v2/msgs/user/:user_id") { request: GetUserMsgsRequest =>
    val user_id = request.request.user.userId
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)

    msgService.getUserMsgList(user_id,request.user_id,limits, offset).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(GetAllCircleMsgsResponse("success", "", resp,""))
    }
  }

  //获取用户消息列表
  get("/v2/user/notice") { request: GetUserMsgNoticeRequest =>
    val userId = request.request.user.userId
    msgService.getUserNoticeList(userId).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
  }

  //获取用户消息数量
  get("/v2/user/notice/count") { request: GetUserMsgNoticeRequest =>
    val userId = request.request.user.userId
    msgService.getUseUserMsgCount(userId).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
  }

  get("/v2/msgs/attention") { request: GetUserAttentionMsgsRequest =>
    val userId = request.request.user.userId
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)
    msgService.getUserAttentionMsg(userId,limits, offset).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(GetAllCircleMsgsResponse("success", "", resp,""))
    }
  }

  post("/v2/msgs/recommend") { request: PostRecommendMsgRequest =>
    msgService.recommendMsg(request).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
  }

}
