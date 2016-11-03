package com.wisedu.next.api.domains


import java.util.UUID

import org.joda.time.DateTime


case class GetUserResp(user_id:UUID,self_url: String, img_url: Option[String], alias: Option[String],
                       password: Option[String], name: Option[String], sex: Option[Int], college_id: Option[String],
                       college_name: Option[String], phone_no: Option[String], home: Option[String], love_status: Option[String],
                       depart: Option[String], departId: Option[String], majors: Option[String], cla: Option[String],
                       dorm: Option[String], degree: Option[String], mschool: Option[String], sign: Option[String],
                       birth_date: Option[DateTime], fresh_date: Option[String], interests: Option[String],
                       actives: Option[String], ctime: Option[DateTime], tokens: Option[String],
                       device_id: Option[String],sns: Set[PostSnsResp], college_short_name: Option[String],
                       backgroundImg: Option[String], depart_short_name: Option[String],role:Option[Int])

case class GetUserResponse(status: String, errMsg: String, user: Option[GetUserResp])


case class GetUserTagsResponse(user_id: UUID, tags: Seq[String])

case class PostSnsResp(openid: String, sns_type: Int, sns_code: String, sns_alias: String)

case class PostUserResp(user_id:UUID,self_url: String, img_url: Option[String], alias: Option[String],
                        password: Option[String], name: Option[String], sex: Option[Int], college_id: Option[String],
                        college_name: Option[String], phone_no: Option[String], home: Option[String], love_status: Option[String],
                        depart: Option[String], majors: Option[String], cla: Option[String],
                        dorm: Option[String], degree: Option[String], mschool: Option[String], sign: Option[String],
                        birth_date: Option[DateTime], fresh_date: Option[String], interests: Option[String],
                        actives: Option[String], ctime: Option[DateTime], tokens: Option[String],
                        device_id: Option[String], sns: Set[PostSnsResp], college_short_name: Option[String],
                        backgroundImg: Option[String], depart_short_name: Option[String],role:Option[Int])

case class PostUsersResponse(status: String, errMsg: String, method: String, user: Option[PostUserResp])

case class PostIdentUserResp(id: UUID, phone_no: Option[String], self_url: String, alias: Option[String],
                             openid: Option[String], sns_type: Option[Int])

case class PostTokenResp(method: String, expires_at: DateTime, user: PostIdentUserResp, audit_ids: String)

case class PostAuthTokensResponse(status: String, errMsg: String, token: Option[PostTokenResp])

case class GetValidityResp(status: String)

case class GetUsersValidityResponse(validity: GetValidityResp)

case class PostUserRegSendMsgResponse(phone: PostUserRegPhone, status: String)

case class PostUserRegVerifyResponse(phone_no: String, auth_code: String, status: String)

case class PostUserRegVerifyAuthCodeResponse(verify: PostUserRegVerifyResponse)

case class PutUserSnsResp(status: String,errCode:String, errMsg:String)

case class PutUserUnBindSnsResponse(sns: PutUserSnsResp)

case class PutUserBindSnsResponse(sns: PutUserSnsResp)

case class PostUserImgResponse(user_id: UUID, img_url: String)