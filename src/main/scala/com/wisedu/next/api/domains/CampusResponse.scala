package com.wisedu.next.api.domains

case class CampusNjxzAuthResp(success: Boolean, message: Option[String], token: Option[String])


case class CampusNjxzUserInfo(WID: String, NC: String, XM: String, RXRQ: Option[Long], YX: String, YXID: String = "",
                              XB: Int, CSRQ: String, SJ: String = "", SF: Int = 0, AVATAR_URL: Option[String])

case class CampusNjxzUserInfoResp(success: Boolean, message: Option[String], property: Option[CampusNjxzUserInfo])