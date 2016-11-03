package com.wisedu.next.api.controllers

import javax.inject.Inject

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.wisedu.next.api.domains.{PostTagNameValidityRequest, PostTagNameValidityResp, PostTagNameValidityResponse}
import com.wisedu.next.api.services.TagService

/**
  * Version: 1.1
  * Author: pattywgm 
  * Time: 16/5/20 上午9:32
  * Desc:
  */
class TagsController extends Controller {

  @Inject var tagService: TagService = _

  //验证标签是否存在
  post("/v2/tag/validity") { request: PostTagNameValidityRequest =>
    tagService.tagValidity(request).map {
      resp =>
        if(resp) response.ok.json(PostTagNameValidityResponse(PostTagNameValidityResp("success")))
        else response.ok.json(PostTagNameValidityResponse(PostTagNameValidityResp("fail")))
    }
  }


  // 查询用户标签
  get("/v2/tag/userTags") { request: Request =>
    tagService.getUserTags(request).map {
      resp => response.ok.json(resp)
    }
  }

}
