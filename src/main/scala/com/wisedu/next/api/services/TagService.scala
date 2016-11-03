package com.wisedu.next.api.services

import javax.inject.Inject

import com.google.inject.Singleton
import com.twitter.finagle.http.Request
import com.twitter.util.Future
import com.wisedu.next.api.domains.{GetUserTagsResponse, PostTagNameValidityRequest}
import com.wisedu.next.api.filters.UserContext._
import com.wisedu.next.services.UserBaseService

/**
  * Version: 1.1
  * Author: pattywgm 
  * Time: 16/6/23 上午10:48
  * Desc:
  */

@Singleton
class TagService {
  @Inject var serviceFunctions: ServiceFunctions = _
  @Inject var userBaseService: UserBaseService = _

  //验证标签是否存在
  def tagValidity(request: PostTagNameValidityRequest):Future[Boolean] = {
    serviceFunctions.varifyTag(request.tag.tag_name).map {
      verifyInfo =>
        if (verifyInfo)
          true
        else
          false
    }
  }

  // 查询用户标签
  def getUserTags(request: Request) :Future[GetUserTagsResponse] = {
    val userId = request.user.userId
    userBaseService.getUserTagsByUserId(userId).map {
      userTags => GetUserTagsResponse(userId, userTags.map {
        userTag => userTag.tagId
      })
    }
  }
}
