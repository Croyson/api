package com.wisedu.next.api.services

import javax.inject.Singleton

import com.google.inject.Inject
import com.twitter.util.Future
import com.wisedu.next.api.domains.{GetDepartInfoResp, GetCollegesResponse, GetCollegesRequest, GetDeparts}
import com.wisedu.next.services.CollegeBaseService

/**
  * Version: 1.1
  * Author: pattywgm 
  * Time: 16/6/23 上午10:27
  * Desc:
  */

@Singleton
class CollegesService {
  @Inject var collegeBaseService: CollegeBaseService = _
  @Inject var serviceFunctions: ServiceFunctions = _

  // 获取学校信息
  def getColleges(request: GetCollegesRequest): Future[GetCollegesResponse] ={
    val limits = request.limits.getOrElse(20)
    val offSet = request.offset.getOrElse(0)
    val region = request.region.getOrElse("南京")

    collegeBaseService.collColleges(limits, offSet, region).map {
      colleges => serviceFunctions.toGetCollegesResponse(colleges)
    }
  }

  // 获取院系
  def getDeparts(request: GetDeparts): Future[GetDepartInfoResp] = {
    collegeBaseService.getDepartsByCollegeId(request.collegeId).map {
      departs => serviceFunctions.toGetDepartInfoResp(departs)
    }
  }
}
