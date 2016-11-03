package com.wisedu.next.api.domains


import java.util.UUID

import com.twitter.finagle.http.Request
import com.twitter.finatra.request.{FormParam, QueryParam, RequestInject, RouteParam}

case class PutFeedShareRequest(@RequestInject request: Request, @RouteParam feed_id: UUID, share_type: String, share_url: String, src_value:String)

case class PutFeedLikeRequest(@RequestInject request: Request, @RouteParam feed_id: UUID, op_type:Int, op_msg:Option[String])

case class PutFeedCollectRequest(@RequestInject request: Request, @RouteParam feed_id: UUID)

case class PostFeedReadRequest(@RequestInject request: Request, @RouteParam feed_id: UUID, @FormParam read_type:String, @FormParam src_value: UUID)

case class GetChannelRequest(@RequestInject request: Request,@RouteParam channel_id: String, @QueryParam limits: Option[Int], @QueryParam offset: Option[Int])

case class GetCirclePosterRequest(@RequestInject request: Request,@RouteParam circle_id: String, @QueryParam limits: Option[Int], @QueryParam offset: Option[Int])

case class GetLiveRequest(@RequestInject request: Request,@QueryParam limits: Option[Int], @QueryParam offset: Option[Int])

case class GetChannelsRequest(@RequestInject request: Request, @QueryParam college_id: Option[String],@QueryParam limits: Option[Int], @QueryParam offset: Option[Int])

case class GetFeedInfoRequest(@RequestInject request: Request, @RouteParam feed_id: UUID, @QueryParam read_type:String,
                              @QueryParam src_value: Option[String], @QueryParam push_value: Option[String], @QueryParam ip_addr: Option[String])

case class PostFeedbackReq(content: String, contact: Option[String])

case class PostFeedbackRequest(@RequestInject request: Request, feedback: PostFeedbackReq)

