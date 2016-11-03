package com.wisedu.next.api.controllers

import javax.inject.Inject

import com.twitter.finatra.http.Controller
import com.wisedu.next.api.domains.{GetCircleNoticeRequest, GetCirclesRequest}
import com.wisedu.next.api.services.CircleService

/**
  * Version: 1.1
  * Author: pattywgm 
  * Time: 16/7/6 上午11:01
  * Desc:
  */
class CircleController extends Controller{

  @Inject var circleService: CircleService = _

  // 获取圈子列表
  get("/v2/circles"){request: GetCirclesRequest =>
    circleService.getCircles(request).map{
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
//    GetCirclesResponse("success","", List(GetCirclesResp("/v2/bestie/msgs","闺蜜圈","/v2/statics/imgs/46564454.jpg")))
  }

  //获取圈子公告以及管理员
  get("/v2/circle/notice"){request: GetCircleNoticeRequest =>
    circleService.getCircleNotice(request).map{
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
  }


  // 获取推荐圈子列表
  get("/v2/circles/recommend"){request: GetCirclesRequest =>
    circleService.getCircles(request).map{
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
//    GetCirclesResponse("success","", List(GetCirclesResp("/v2/bestie/msgs","闺蜜圈","/v2/statics/imgs/46564454.jpg")))
  }




}


