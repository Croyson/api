package com.wisedu.next.api.domains

/**
  * Version: 1.1
  * Author: pattywgm 
  * Time: 16/7/6 上午11:06
  * Desc: 虚拟组相关请求返回
  */

case class GetCirclesResp(self_url: String, id:String, name: String, img_url: String, is_anonymous:Int,
                          is_realName_post:Int,is_realName_respond:Int,is_default_save:Int,adminUser:String)

case class GetCirclesResponse(status: String, errMsg: String, circles: Seq[GetCirclesResp])

case class CircleNotice(circle_id:String,circle_notice:String,admin_user:String)

case class GetCircleNoticeResponse(status: String, errMsg: String,circleNotice:Option[CircleNotice])

