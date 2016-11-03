package com.wisedu.next.api.controllers

import com.google.inject.Inject
import com.twitter.finatra.http.Controller
import com.wisedu.next.api.domains.{GetDeparts, GetCollegesRequest}
import com.wisedu.next.api.services.{CollegesService, ServiceFunctions}


class CollegesController extends Controller {

  @Inject var collegesService: CollegesService = _

  // 获取学校信息
  get("/v2/colleges"){ request: GetCollegesRequest =>
    collegesService.getColleges(request).map {
          collegesResp => response.ok.json(collegesResp)
        }
  }

  // 获取院系
  get("/v2/college/departs/:collegeId"){ request: GetDeparts =>
    collegesService.getDeparts(request).map{
      resp => response.ok.json(resp)
    }
  }
}