package com.wisedu.next.api.domains

import java.util.UUID

import com.twitter.finagle.http.Request
import com.twitter.finatra.request._

/**
 * Version: 1.1
 * Author: pattywgm
 * Time: 16/7/6 上午9:11
 * Desc: 新鲜事(消息)请求
 */

case class PostMsgReq(feed_id: Option[String], p_msg_id: Option[String], content: String, msg_img: Option[String],
                      update_type: Int, thresh_hold: Option[Int], group_id: Option[String],reply_msg_id:Option[UUID],
                       linkTitle: Option[String], linkImg: Option[String], linkUrl: Option[String])

case class PostMsgRequest(@RequestInject request: Request, method: String, is_anonymous: Int, msg: PostMsgReq)

case class GetMsgInfoRequest(@RequestInject request: Request, @RouteParam msg_id: String)

case class GetMsgExpressListRequest(@RequestInject request: Request, @RouteParam msg_id: String, @QueryParam method: String,
                                    @QueryParam model_id: Option[String], @QueryParam select_id: Option[String], @QueryParam limits: Option[Int],
                                    @QueryParam offset: Option[Int])

case class GetCircleMsgsRequest(@RequestInject request: Request, @RouteParam group_id: String,@QueryParam college_id: Option[String], @QueryParam limits: Option[Int], @QueryParam offset: Option[Int])

case class GetUserMsgsRequest(@RequestInject request: Request, @RouteParam user_id: UUID, @QueryParam limits: Option[Int], @QueryParam offset: Option[Int])

case class GetUserAttentionMsgsRequest(@RequestInject request: Request, @QueryParam limits: Option[Int], @QueryParam offset: Option[Int])


case class GetTopMsgsRequest(@RequestInject request: Request, @QueryParam college_id: Option[String], @RouteParam group_id: Option[String])

case class GetCollegeMsgsRequest(@RequestInject request: Request, @QueryParam college_id: Option[String],@QueryParam is_recommend: Option[String], @QueryParam limits: Option[Int], @QueryParam offset: Option[Int])

case class GetFollowingUsersRequest(@RequestInject request: Request, @RouteParam user_id: UUID, @QueryParam limits: Option[Int], @QueryParam offset: Option[Int])

case class PostRecommendMsgRequest(@RequestInject request: Request, msg_id: UUID, status: Int)

case class GetUserStatRequest(@RequestInject request: Request, @RouteParam user_id: UUID)

case class UserStatRequest(@RequestInject request: Request, @QueryParam user_id: UUID)

case class GetUserMsgNoticeRequest(@RequestInject request: Request)

