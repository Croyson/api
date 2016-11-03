package com.wisedu.next.api.domains

/**
  * Version: 1.1
  * Author: pattywgm 
  * Time: 16/6/2 下午3:07
  * Desc:
  */

import com.twitter.finagle.http.Request
import com.twitter.finatra.request._

case class ImgUploadRequest(@RequestInject request: Request, @RouteParam id: Option[String])

