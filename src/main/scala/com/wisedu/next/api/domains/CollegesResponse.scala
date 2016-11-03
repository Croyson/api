package com.wisedu.next.api.domains


case class GetCollegeGpsResp(lat: Float, lng: Float)

case class GetCollegeResp(self_url: String, college_id: String, name: String, ename: String, short_name: String, img_url: String,
                          user_num: Long, region: String, gps: GetCollegeGpsResp)

case class GetCollegesResponse(colleges: Seq[GetCollegeResp])

case class DepartInfo(departId:String,collegeId:String,departName:String,descr:String,sortNo:Int)

case class GetDepartInfoResp(departs: Seq[DepartInfo])