package com.wisedu.next.api.domains

/**
  * Version: 2.0
  * Author: pattywgm 
  * Time: 16/5/18 下午5:23
  * Desc:
  */


import com.twitter.finagle.http.Request
import com.twitter.finatra.request.{QueryParam, RequestInject, RouteParam}

case class GetPrizeTopicRequest(@RequestInject request: Request, @QueryParam college_id: Option[String], @QueryParam limits: Option[Int], @QueryParam offset: Option[Int])

case class GetThisWeekTopicRequest(@RequestInject request: Request, @QueryParam college_id: Option[String], @QueryParam limits: Option[Int], @QueryParam offset: Option[Int])

case class GetTopicRequest(@RequestInject request: Request, @QueryParam college_id: Option[String], @RouteParam id: String, @QueryParam read_type:String,
                           @QueryParam src_value: Option[String], @QueryParam push_value: Option[String], @QueryParam ip_addr: Option[String])