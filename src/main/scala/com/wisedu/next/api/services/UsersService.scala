package com.wisedu.next.api.services

import java.util.UUID
import javax.inject.{Inject, Singleton}

import com.taobao.api.response.{OpenimImmsgPushResponse, OpenimUsersAddResponse, OpenimUsersDeleteResponse, OpenimUsersUpdateResponse}
import com.twitter.finatra.annotations.Flag
import com.twitter.util.Future
import com.wisedu.next.api.domains._
import com.wisedu.next.api.filters.UserContext._
import com.wisedu.next.models.UserSnsInfo
import com.wisedu.next.services.{BaseFunctions, CollegeBaseService, StaticBaseService, UserBaseService}
import org.joda.time.DateTime

/**
 * Version: 1.1
 * Author: pattywgm
 * Time: 16/6/29 上午10:04
 * Desc:
 */

@Singleton
class UsersService {
  @Inject
  @Flag("local.doc.root") var filePath: String = _
  @Inject var userBaseService: UserBaseService = _
  @Inject var collegeBaseService: CollegeBaseService = _
  @Inject var serviceFunctions: ServiceFunctions = _
  @Inject var aliSMSFcServiceFunction: AliSMSFcServiceFunction = _
  @Inject var staticBaseService: StaticBaseService = _
  @Inject var campusService: CampusService = _
  @Inject var imUserService: ImUserService = _

  // 用户信息更新
  def userInfoUpdate(request: PutUserInfoRequest): Future[GetUserResponse] = {
    try {
      userBaseService.getUserById(request.request.user.userId).flatMap {
        case Some(oldUser) =>
          //用户信息修改
          serviceFunctions.getModifyUserInfo(request.request.user.userId, request, oldUser).flatMap {
            user =>
              imUserService.updImUsers(Seq(user))
              userBaseService.updUser(user).flatMap {
                result => {
                  userBaseService.sysUserTags(user.userId)
                  serviceFunctions.toGetUserResponse(user)
                }
              }.map {
                item => item
              }
          }
        case None => Future {
          GetUserResponse("fail", "The user does not exists!", None)
        }
      }
    } catch {
      case _: Exception => Future {
        GetUserResponse("fail", "Update user info has some exception!", None)
      }
    }
  }

  //用户信息查询
  def userInfoSearch(request: GetUserInfoRequest): Future[GetUserResponse] = {
    userBaseService.getUserById(request.user_id).flatMap {
      case Some(user) => serviceFunctions.toGetUserResponse(user)
      case None => Future {
        GetUserResponse("fail", "The user fly away...", None)
      }
    }
  }

  //用户密码修改
  def modifyPassword(request: PostModifyPasswordReq): Future[GetUserResponse] = {
    userBaseService.getUserByPhoneNo(request.phone_no).flatMap {
      case Some(user) => aliSMSFcServiceFunction.sendRegVerifySms(request.phone_no, request.auth_code).flatMap {
        rst => if (rst) {
          userBaseService.updUserPassword(user.userId, BaseFunctions.md5Encryption(request.new_password, "cpdaily123")).flatMap {
            item => serviceFunctions.toGetUserResponse(user)
          }
        } else {
          Future {
            GetUserResponse("fail", "auth code error", None)
          }
        }
      }
      case None => Future {
        GetUserResponse("fail", "the user is not exit", None)
      }
    }
  }


  //用户注册
  def postUserInfo(request: PostUsersRequest): Future[PostUsersResponse] = {
    val method = request.method
    if ("sns".equals(method)) {
      //微信, 微博, QQ 用户
      val snsType = request.user.sns_type
      val openId = request.user.openid
      if (snsType.isDefined && openId.isDefined) {
        userBaseService.getSnsByOpenId(openId.get, snsType.get).flatMap {
          case Some(snsInfo) => userBaseService.getUserById(snsInfo.userId).flatMap {
            case Some(user) =>
              userBaseService.updUserStatus(user.deviceId, user.userId.toString)
              userBaseService.getSnsByUserId(user.userId).flatMap {
                snss => serviceFunctions.toPostUsersResponse("", "isExist", user, snss, user.alias)
              }
            case None => Future {
              PostUsersResponse("fail", "data is wrong", "", None)
            }
          }
          case None =>
            serviceFunctions.toUser(request, false).flatMap {
              user =>
                val userSnsInfo = serviceFunctions.toUserSnsInfo(user.userId, request)
                userBaseService.insUser(user).flatMap {
                  result =>
                    imUserService.addImUsers(Seq(user))
                    userBaseService.sysUserTags(user.userId)
                    userBaseService.insUserSnsInfo(userSnsInfo)
                }.flatMap {
                  result => userBaseService.getSnsByUserId(user.userId).flatMap {
                    snss => serviceFunctions.toPostUsersResponse("", "isExist", user, snss, user.alias)
                  }
                }
            }
        }
      } else {
        Future {
          PostUsersResponse("fail", "field is required", "", None)
        }
      }
    } else if ("anonymous".equals(method)) {
      val deviceId = request.user.device_id
      userBaseService.getByDeviceId(deviceId).flatMap {
        users => {
          val usersT = users.filter(_.isAnonymousUser.equals(1))
          if (usersT.isEmpty) {
            serviceFunctions.toUser(request, true).flatMap {
              user =>
                userBaseService.getUserByAlias(user.alias).flatMap {
                  case Some(user) =>
                    val alias = user.alias + DateTime.now
                    userBaseService.insUser(user).flatMap {
                      result => {
                        userBaseService.sysUserTags(user.userId)
                        serviceFunctions.toPostUsersResponse(method, "", user, Seq(), alias)
                      }
                    }
                  case None =>
                    userBaseService.insUser(user).flatMap {
                      result => {
                        userBaseService.sysUserTags(user.userId)
                        serviceFunctions.toPostUsersResponse(method, user, None)
                      }
                    }
                }
            }
          } else {
            serviceFunctions.toUser(request, true).flatMap {
              user =>
                userBaseService.updUserCollege(user.collegeId, user.collegeName, usersT.head.userId.toString).flatMap {
                  result => userBaseService.getUserById(usersT.head.userId).flatMap {
                    newUser => userBaseService.sysUserTags(usersT.head.userId)
                      userBaseService.updUserStatus(usersT.head.deviceId, usersT.head.userId.toString)
                      serviceFunctions.toPostUsersResponse(method, newUser.get, None)
                  }
                }
            }
          }
        }
      }
    } else if ("phone".equals(method)) {
      //手机号注册用户
      if (!(request.user.alias.getOrElse("").isEmpty || request.user.phone_no.getOrElse("").isEmpty
        || request.user.password.getOrElse("").isEmpty)
        || request.user.auth_code.getOrElse("").isEmpty) {

        userBaseService.getUserByPhoneNo(request.user.phone_no.get).flatMap {
          case Some(user) =>
            Future {
              PostUsersResponse("fail", "uses has exists", "", None)
            }
          case None => aliSMSFcServiceFunction.sendRegVerifySms(request.user.phone_no.getOrElse(""), request.user.auth_code.getOrElse("")).flatMap {
            item => if (item) {
              val verifyName_f = serviceFunctions.verifyName(request.user.alias.getOrElse(""), "")
              val verifyPhone_f = serviceFunctions.verifyPhone(request.user.phone_no.getOrElse(""), "")

              val verifyInfo_f = for {
                verifyName <- verifyName_f
                verifyPhone <- verifyPhone_f
              } yield (verifyName, verifyPhone)

              verifyInfo_f.flatMap {
                verifyInfo =>
                  if (verifyInfo.equals((true, true))) {
                    serviceFunctions.toUser(request, false).flatMap {
                      user =>
                        userBaseService.insUser(user).flatMap {
                          result =>
                            imUserService.addImUsers(Seq(user))
                            userBaseService.sysUserTags(user.userId)
                            userBaseService.updUserStatus(user.deviceId, user.userId.toString)
                            serviceFunctions.toPostUsersResponse(method, user, None)
                        }
                    }
                  } else {
                    Future {
                      PostUsersResponse("fail", "Registration fails", "", None)
                    }
                  }
              }
            } else {
              Future {
                PostUsersResponse("fail", "Registration fails! auth code error", "", None)
              }
            }
          }
        }
      } else {
        Future {
          PostUsersResponse("fail", "Registration fails", "", None)
        }
      }
    } else {
      Future {
        PostUsersResponse("fail", "Registration fails", "", None)
      }
    }
  }

  def verifyUserSnsInfo(request: PostVerifyUsersSnsRequest): Future[PostUsersResponse] = {

    userBaseService.getSnsByOpenId(request.openid, request.sns_type).flatMap {
      case Some(snsInfo) => userBaseService.getUserById(snsInfo.userId).flatMap {
        case Some(user) =>
          userBaseService.updUserStatus(user.deviceId, user.userId.toString)
          userBaseService.getSnsByUserId(user.userId).flatMap {
            snss => serviceFunctions.toPostUsersResponse("", "isExist", user, snss, user.alias)
          }

        case None => Future {
          PostUsersResponse("fail", "notExist", "", None)
        }
      }
      case None => Future {
        PostUsersResponse("fail", "notExist", "", None)
      }
    }
  }

  // 社交网络登陆认证
  def postUserSnsAuth(request: PostAuthTokensRequest): Future[PostAuthTokensResponse] = {
    if (request.auth.identity.method.equals("phone")) {
      val phoneNo = request.auth.identity.user.phone_no
      val password = request.auth.identity.user.password
      serviceFunctions.verifyPhoneInfo(phoneNo, password).map {
        case (userO, errorMsg) => userO match {
          case Some(user) =>
            userBaseService.updUserStatus(user.deviceId, user.userId.toString)
            if (user.tokenS.equals("")) {
              val tokenS = UUID.randomUUID().toString
              userBaseService.updUserToken(user.userId, tokenS)
              PostAuthTokensResponse("success", "", Some(PostTokenResp("none", DateTime.now,
                PostIdentUserResp(user.userId, request.auth.identity.user.phone_no,
                  s"/v2/users/${user.userId}", request.auth.identity.user.alias,
                  request.auth.identity.user.openid, request.auth.identity.user.sns_type), tokenS)))
            } else {
              PostAuthTokensResponse("success", "", Some(PostTokenResp("none", DateTime.now,
                PostIdentUserResp(user.userId, request.auth.identity.user.phone_no,
                  s"/v2/users/${user.userId}", request.auth.identity.user.alias,
                  request.auth.identity.user.openid, request.auth.identity.user.sns_type), user.tokenS)))
            }
          case None =>
            PostAuthTokensResponse("fail", errorMsg, None)
        }
      }
    } else if (request.auth.identity.method.equals("sns")) {
      val openId = request.auth.identity.user.openid
      val snsType = request.auth.identity.user.sns_type
      if (snsType.getOrElse(0) != 4) {
        serviceFunctions.verifySnsInfo(openId, snsType).flatMap {
          case Some(user) => userBaseService.getUserById(user.userId).filter(_.isDefined).map(_.get).map {
            user =>
              userBaseService.updUserStatus(user.deviceId, user.userId.toString)
              if (user.tokenS.equals("")) {
                val tokenS = UUID.randomUUID().toString
                userBaseService.updUserToken(user.userId, tokenS)
                PostAuthTokensResponse("success", "", Some(PostTokenResp("none", DateTime.now,
                  PostIdentUserResp(user.userId, request.auth.identity.user.phone_no,
                    s"/v2/users/${user.userId}", request.auth.identity.user.alias,
                    request.auth.identity.user.openid, request.auth.identity.user.sns_type), tokenS)))
              } else {
                PostAuthTokensResponse("success", "", Some(PostTokenResp("none", DateTime.now,
                  PostIdentUserResp(user.userId, request.auth.identity.user.phone_no,
                    s"/v2/users/${user.userId}", request.auth.identity.user.alias,
                    request.auth.identity.user.openid, request.auth.identity.user.sns_type), user.tokenS)))
              }
          }
          case None =>
            Future {
              PostAuthTokensResponse("fail", "Validation fails", None)
            }
        }
      } else {
        idxAuth(request)
      }
    } else {
      Future {
        PostAuthTokensResponse("fail", "Validation fails", None)
      }
    }
  }


  def idxAuth(request: PostAuthTokensRequest): Future[PostAuthTokensResponse] = {
    val userName = request.auth.identity.user.userName.getOrElse("")
    val password = request.auth.identity.user.password.getOrElse("")
    val deviceId = request.auth.identity.user.device_id
    val snsType = request.auth.identity.user.sns_type.getOrElse(4)
    //验证登录信息
    campusService.getNjxzUserInfoByAuth(userName, password).flatMap {
      rsp => if (rsp.success) {
        val campusUser = rsp.property.getOrElse(campusService.getDefaultUserInfo())
        userBaseService.getSnsByOpenId(campusUser.WID, snsType).flatMap {
          case Some(user) => userBaseService.getUserById(user.userId).flatMap {
            case Some(user) =>
              //if user must be inited
              val resultF = if (user.deviceId.equals("")) {
                campusService.completeUser(campusUser, user, deviceId).flatMap {
                  user => userBaseService.updUser(user).flatMap {
                    result => userBaseService.sysUserTags(user.userId).flatMap {
                      result => userBaseService.updUserStatus(deviceId, user.userId.toString)
                    }
                  }
                }
              } else {
                userBaseService.updUserStatus(deviceId, user.userId.toString)
              }
              resultF.map {
                result =>
                  if (user.tokenS.equals("")) {
                    val tokenS = UUID.randomUUID().toString
                    userBaseService.updUserToken(user.userId, tokenS)
                    PostAuthTokensResponse("success", "isExist", Some(PostTokenResp("sns", DateTime.now,
                      PostIdentUserResp(user.userId, request.auth.identity.user.phone_no,
                        s"/v2/users/${user.userId}", request.auth.identity.user.alias,
                        request.auth.identity.user.openid, request.auth.identity.user.sns_type), tokenS)))
                  } else {
                    PostAuthTokensResponse("success", "isExist", Some(PostTokenResp("sns", DateTime.now,
                      PostIdentUserResp(user.userId, request.auth.identity.user.phone_no,
                        s"/v2/users/${user.userId}", request.auth.identity.user.alias,
                        request.auth.identity.user.openid, request.auth.identity.user.sns_type), user.tokenS)))
                  }
              }
            case None => {
              Future {
                PostAuthTokensResponse("failed", rsp.message.getOrElse(""), None)
              }
            }
          }
          case None =>
            //创建用户
            campusService.toUser(campusUser, deviceId).flatMap {
              user => val userSnsInfo = UserSnsInfo(user.userId, campusUser.WID, "", campusUser.NC, snsType, DateTime.now())
                userBaseService.insUser(user).flatMap {
                  result => {
                    imUserService.addImUsers(Seq(user))
                    userBaseService.sysUserTags(user.userId)
                    userBaseService.insUserSnsInfo(userSnsInfo)
                  }
                }.map {
                  result => val tokenS = UUID.randomUUID().toString
                    userBaseService.updUserToken(user.userId, tokenS)
                    PostAuthTokensResponse("success", "firstUser", Some(PostTokenResp("sns", DateTime.now,
                      PostIdentUserResp(user.userId, request.auth.identity.user.phone_no,
                        s"/v2/users/${user.userId}", request.auth.identity.user.alias,
                        request.auth.identity.user.openid, request.auth.identity.user.sns_type), tokenS)))
                }
            }
        }
      } else {
        Future(PostAuthTokensResponse("failed", rsp.message.getOrElse(""), None))
      }
    }

  }

  def bindUserSns(request: PutUserBindSnsRequest): Future[PutUserBindSnsResponse] = {

    //判断SNS是否被绑定过
    userBaseService.getSnsByOpenId(request.sns.openid, request.sns.sns_type).flatMap {
      case Some(rst) => Future(PutUserBindSnsResponse(PutUserSnsResp("fail", "0001", "openid is exits")))
      case None =>
        if (request.sns.sns_type == 4) {
          //ids验证
          campusService.getNjxzUserInfoByAuth(request.sns.openid, request.sns.password.getOrElse("")).flatMap {
            rsp => if (rsp.success) {
              //封装数据
              val userSnsInfo = UserSnsInfo(request.user_id, request.sns.openid, request.sns.sns_code,
                rsp.property.getOrElse(campusService.getDefaultUserInfo()).XM,
                request.sns.sns_type, DateTime.now())
              userBaseService.insUserSnsInfo(userSnsInfo).map {
                result => PutUserBindSnsResponse(PutUserSnsResp("success", "", ""))
              }
            } else {
              Future(PutUserBindSnsResponse(PutUserSnsResp("failed", "0002", "验证失败:" + rsp.message.getOrElse(""))))
            }
          }
        } else {
          //封装数据
          val userSnsInfo = UserSnsInfo(request.user_id, request.sns.openid, request.sns.sns_code, request.sns.sns_alias,
            request.sns.sns_type, DateTime.now())
          userBaseService.insUserSnsInfo(userSnsInfo).map {
            result => PutUserBindSnsResponse(PutUserSnsResp("success", "", ""))
          }
        }
    }.rescue {
      case e: Exception => Future(PutUserBindSnsResponse(PutUserSnsResp("fail", "0004", "系统繁忙,请稍后再试!")))
    }

  }


  def unBindUserSns(request: PutUserUnBindSnsRequest): Future[PutUserUnBindSnsResponse] = {
    userBaseService.getSnsByUserId(request.user_id).flatMap {
      items => if (items.filter(item => item.openId.equals(request.openid) && item.snsType.equals(request.sns_type)).size == 0) {
        //该用户不存在此openid
        Future(PutUserUnBindSnsResponse(PutUserSnsResp("fail", "0001", "openid is not exits")))
      } else if (items.size == 1) {
        //该用户的最后一条记录不能被解绑
        Future(PutUserUnBindSnsResponse(PutUserSnsResp("fail", "0002", "the last record")))
      } else {
        userBaseService.delUserSnsInfo(request.user_id, request.openid, request.sns_type).map {
          result => PutUserUnBindSnsResponse(PutUserSnsResp("success", "", ""))
        }
      }
    }

  }

  def delUsersToIm(): Future[Seq[OpenimUsersDeleteResponse]] = {
    userBaseService.collUsersSize("", "0", "", "").flatMap {
      userSize =>
        var uploadsF = Seq[Future[OpenimUsersDeleteResponse]]()
        for (i <- 0 to userSize / 100) {
          val uploadF = userBaseService.collUsers("", "0", "", "", 100, i * 100).flatMap(
            users => imUserService.delImUsers(users)
          )
          uploadsF = uploadsF :+ uploadF
        }
        Future.collect(uploadsF)
    }
  }

  def addUsersToIm(userIds: String): Future[OpenimUsersAddResponse] = {
    userBaseService.collUsersByIds(userIds).flatMap {
      users => imUserService.addImUsers(users)
    }.rescue {
      case e: Exception =>
        throw e
    }
  }


  def initUsersToIm(): Future[Seq[OpenimUsersAddResponse]] = {
    userBaseService.collUsersSize("", "0", "", "").flatMap {
      userSize =>
        var uploadsF = Seq[Future[OpenimUsersAddResponse]]()
        for (i <- 0 to userSize / 100) {
          val uploadF = userBaseService.collUsers("", "0", "", "", 100, i * 100).flatMap(
            users => imUserService.addImUsers(users)
          )
          uploadsF = uploadsF :+ uploadF
        }
        Future.collect(uploadsF)
    }
  }


  def updUsersToIm(): Future[Seq[OpenimUsersUpdateResponse]] = {
    userBaseService.collUsersSize("", "0", "", "").flatMap {
      userSize =>
        var uploadsF = Seq[Future[OpenimUsersUpdateResponse]]()
        for (i <- 0 to userSize / 100) {
          val uploadF = userBaseService.collUsers("", "0", "", "", 100, i * 100).flatMap(
            users => imUserService.updImUsers(users)
          )
          uploadsF = uploadsF :+ uploadF
        }
        Future.collect(uploadsF)
    }
  }

  def sendMsgToIm(from: String, content: String): Future[Seq[OpenimImmsgPushResponse]] = {
    userBaseService.collUsersSize("", "0", "", "").flatMap {
      userSize =>
        var sendMsgsF = Seq[Future[OpenimImmsgPushResponse]]()
        for (i <- 0 to userSize / 100) {
          val sendMsgF = userBaseService.collUsers("", "0", "", "", 100, i * 100).flatMap(
            users =>
              imUserService.pushMsgToUsers(from, users.map(_.userId.toString), content)
          )
          sendMsgsF = sendMsgsF :+ sendMsgF
        }
        Future.collect(sendMsgsF)
    }
  }

}
