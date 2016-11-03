package com.wisedu.next.api.controllers

import javax.inject.Inject

import com.twitter.finatra.http.Controller
import com.wisedu.next.api.domains.GetChannelsRequest
import com.wisedu.next.api.services.GroupService

/**
  * Version: 1.1
  * Author: pattywgm 
  * Time: 16/7/6 上午11:01
  * Desc:
  */
class GroupController extends Controller{

  @Inject var groupService: GroupService = _

  // 获取频道集合
  get("/v2/channels") { request: GetChannelsRequest =>
    groupService.getChannels(request).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
  }

}
