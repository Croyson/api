package com.wisedu.next.api.domains

/**
  * Version: 2.0
  * Author: pattywgm 
  * Time: 16/5/18 下午1:24
  * Desc:
  */


import com.twitter.finagle.http.Request
import com.twitter.finatra.request._
import org.joda.time.DateTime
import java.util.UUID

case class PostUserReq(openid: Option[String], sns_type: Option[Int], sns_code: Option[String], device_id: String,
                       img_url: Option[String], alias: Option[String],
                       password: Option[String], name: Option[String], sex: Option[Int], college: String,
                       phone_no: Option[String], home: Option[String], love_status: Option[String],
                       depart: Option[String], majors: Option[Set[String]], cla: Option[String], auth_code: Option[String],
                       dorm: Option[String], mschool: Option[String], sign: Option[String],
                       birth_date: Option[DateTime], fresh_date: Option[String], interests: Option[Option[String]],
                       actives: Option[Set[String]],backgroundImg: Option[String])

case class PostUsersRequest(method: String, user: PostUserReq)

case class PostVerifyUsersSnsRequest(openid:String,sns_type:Int,device_id: String)

case class PutUserReq(img_url: Option[String],img_big_url: Option[String], alias: Option[String],
                      password: Option[String], name: Option[String], sex: Option[Int], phone_no: Option[String],
                      home: Option[String], love_status: Option[String], college: Option[String],
                      depart: Option[String], majors: Option[Set[String]], cla: Option[String],
                      dorm: Option[String], mschool: Option[String], sign: Option[String],
                      birth_date: Option[DateTime], fresh_date: Option[String], interests: Option[Set[String]],
                      actives: Option[Set[String]],backgroundImg: Option[String])

case class PutUserInfoRequest(@RequestInject request: Request, user: PutUserReq)

case class GetUserInfoRequest(@RouteParam user_id: UUID)

case class PostIdentUserReq(alias: Option[String], password: Option[String], phone_no: Option[String],
                            openid: Option[String], sns_type: Option[Int],device_id:String,
                             userName:Option[String])

case class PostAuthIdentReq(method: String, user: PostIdentUserReq)

case class PostAuthReq(identity: PostAuthIdentReq)

case class PostAuthTokensRequest(auth: PostAuthReq)

case class PostUsersPhoneReq(phone_no: String,user_id:Option[String])

case class PostUsersPhoneValidityRequest(validity: PostUsersPhoneReq)

case class PostUsersAliasReq(alias: String,user_id:Option[String])

case class PostUsersAliasValidityRequest(validity: PostUsersAliasReq)

case class PostUserRegPhone(phone_no: String)

case class PostUserRegSendMsgRequest(phone: PostUserRegPhone,method:Int=0)

case class PostUserRegVerifyReq(phone_no: String, auth_code: String)

case class PostUserRegVerifyAuthCodeRequest(verify: PostUserRegVerifyReq)

case class PutBindSnsReq(sns_type: Int, openid: String, sns_code: String, sns_alias: String,password:Option[String])

case class PutUserBindSnsRequest(@RouteParam user_id: UUID, sns: PutBindSnsReq)

case class PutUserUnBindSnsRequest(@RouteParam user_id: UUID, sns_type: Int, openid: String)

case class PostUserImgRequest(@RequestInject request: Request, @RouteParam user_id: UUID)


case class PostModifyPasswordReq(phone_no:String,auth_code:String,new_password:String)

case class PostIdsRegRequest(@RequestInject request: Request,token: String)

case class PushMsgRequest(@RequestInject request: Request, from: String, to: Seq[String], content:String)