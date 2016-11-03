package com.wisedu.next.api.domains

import com.twitter.finagle.http.Request
import com.twitter.finatra.request._

/**
  * Version: 1.1
  * Author: pattywgm 
  * Time: 16/7/6 上午11:06
  * Desc: 圈子相关请求
  */

case class GetCirclesRequest(@RequestInject request: Request, @QueryParam college_id: Option[String], limits: Option[Int], offset: Option[Int])

case class GetCircleNoticeRequest(@RequestInject request: Request, @QueryParam college_id: Option[String],@QueryParam is_recommend: Option[String],@QueryParam circle_id:Option[String])