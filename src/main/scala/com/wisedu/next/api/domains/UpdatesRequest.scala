package com.wisedu.next.api.domains

/**
  * Version: 2.0
  * Author: croyson
  * Time: 16/6/15 下午5:23
  * Desc:
  */


import java.util.UUID

import com.twitter.finagle.http.Request
import com.twitter.finatra.request.{QueryParam, RequestInject, RouteParam}

case class UpdatesDelReq(@RequestInject request: Request, @RouteParam update_id: UUID)

case class GetUpdatesRequest(@RequestInject request: Request, @RouteParam feed_id: UUID, @QueryParam limits: Option[Int], @QueryParam offset: Option[Int],
                             @QueryParam method: String, @QueryParam update_id: Option[String])

case class GetUpdatesTopRequest(@RequestInject request: Request,@RouteParam feed_id: UUID, @QueryParam limits: Option[Int])

case class PostUpdateReq(feed_id: UUID, update_id: Option[String], content: String, img_urls: Option[String], update_type: Int, thresh_hold: Option[Int])

case class PostUpdatesRequest(@RequestInject request: Request, method: String, is_anonymous:Int, update: PostUpdateReq)

case class PutUpdateLikeRequest(@RequestInject request: Request, @RouteParam update_id: UUID, op_type: Int)

case class GetFeedUpdateRequest(@RequestInject request: Request, @RouteParam update_id: UUID)

case class GetUpdateModeRequest(@RequestInject request: Request, @RouteParam feed_id: String)

case class GetMsgUpdatesRequest(@RequestInject request: Request, @RouteParam msg_id: String, @QueryParam sort_type: Option[Int], @QueryParam limits: Option[Int], @QueryParam offset:Option[Int])
