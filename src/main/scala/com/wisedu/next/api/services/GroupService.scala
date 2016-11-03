package com.wisedu.next.api.services

import javax.inject.Inject

import com.google.inject.Singleton
import com.twitter.util.Future
import com.wisedu.next.api.filters.UserContext._
import com.wisedu.next.api.domains._
import com.wisedu.next.services.GroupBaseService

/**
  * Version: 1.1
  * Author: pattywgm 
  * Time: 16/7/6 下午3:02
  * Desc:
  */

@Singleton
class GroupService {

  @Inject var groupBaseService: GroupBaseService = _
  @Inject var serviceFunctions: ServiceFunctions = _


  // 获取频道集合
  def getChannels(request: GetChannelsRequest): Future[GetChannelsResponse] = {
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)
    val collegeId = request.college_id.getOrElse(request.request.user.collegeId)
    groupBaseService.collGroup(collegeId, "", "5,0", limits, offset).map {
      groups => groups.map {
        group =>
          serviceFunctions.toGetChannelRsp(group)
      }
    }.map(item => GetChannelsResponse(item))
  }


}
