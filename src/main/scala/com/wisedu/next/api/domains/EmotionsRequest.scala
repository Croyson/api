package com.wisedu.next.api.domains

import com.twitter.finagle.http.Request
import com.twitter.finatra.request._

/**
  * Version: 1.1
  * Author: pattywgm 
  * Time: 16/6/28 下午1:41
  * Desc:
  */

case class PostEmotionsRequest(@RequestInject request: Request, method: String, feed_id: Option[String], update_id: Option[String], select_id: String)

case class GetEmotions(@RequestInject request: Request, @QueryParam method: String, @RouteParam feed_id: Option[String], @QueryParam update_id: Option[String])
