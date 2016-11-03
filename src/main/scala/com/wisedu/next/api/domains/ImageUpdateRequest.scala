package com.wisedu.next.api.domains

/**
  * Version: 1.0
  * Author: pattywgm 
  * Time: 16/5/26 下午8:01
  * Desc:
  */

import com.twitter.finagle.http.Request
import com.twitter.finatra.request.RequestInject


case class UpdateRequest(@RequestInject request: Request)

