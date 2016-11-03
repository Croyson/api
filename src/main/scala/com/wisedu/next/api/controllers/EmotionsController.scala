package com.wisedu.next.api.controllers

import javax.inject.Inject

import com.twitter.finatra.http.Controller
import com.wisedu.next.api.domains.{GetEmotions, PostEmotionsRequest}
import com.wisedu.next.api.services.EmotionsService

/**
  * Version: 1.1
  * Author: pattywgm 
  * Time: 16/6/28 下午1:43
  * Desc:
  */
class EmotionsController extends Controller{

  @Inject var emotionsService:EmotionsService = _

  // 表达情绪
  post("/v2/feeds/:feed_id/emotions") { request: PostEmotionsRequest =>
    emotionsService.postEmotion(request).map{
      resp => response.ok.json(resp)
    }
  }

  //获取资讯情绪统计数据
  get("/v2/feeds/:feed_id/emotion") { request: GetEmotions =>
    emotionsService.getEmotions(request).map{
      resp => response.ok.json(resp)
    }
  }

  // 发布情绪,统一接口
  post("/v2/emotions"){request: PostEmotionsRequest =>
    emotionsService.postEmotion(request).map{
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
  }

  //获取资讯情绪统计数据,统一接口
  get("/v2/emotions") { request: GetEmotions =>
    emotionsService.getEmotions(request).map{
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
  }

}
