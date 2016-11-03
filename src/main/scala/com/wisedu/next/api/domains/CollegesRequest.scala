package com.wisedu.next.api.domains


import com.twitter.finatra.request._

case class GetCollegesRequest(@QueryParam limits: Option[Int], @QueryParam offset: Option[Int], @QueryParam region: Option[String])

case class GetDeparts(@RouteParam collegeId: String)