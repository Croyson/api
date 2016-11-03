package com.wisedu.next.api.domains

import com.twitter.finagle.http.Request
import com.twitter.finatra.request._


case class GetMediaRequest(@RequestInject request: Request, @RouteParam media_id: String)

case class PutMediaRequest(@RequestInject request: Request, @RouteParam media_id: String)

case class GetMediasRequest(@RequestInject request: Request, @QueryParam limits: Option[Int],
                            @QueryParam offset: Option[Int])

case class GetMediaFeedsRequest(@RequestInject request: Request, @RouteParam media_id: String, @QueryParam limits: Option[Int],
                                @QueryParam offset: Option[Int])