package com.wisedu.next.api.domains

import java.util.UUID

import com.twitter.finagle.http.Request
import com.twitter.finatra.request._

/**
  * Version: 1.1
  * Author: croyson
  * Time: 16/7/6 上午11:06
  * Desc: 圈子用户相关请求
  */

case class PostCircleUserRequest(@RequestInject request: Request,user_id:UUID)