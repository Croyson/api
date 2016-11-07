package com.wisedu.next.api.controllers

import java.util.UUID
import javax.inject.Inject

import com.twitter.finagle.http.{Cookie => FinagleCookie, Request}
import com.twitter.finatra.annotations.Flag
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.fileupload.MultipartItem
import com.twitter.finatra.http.request.RequestUtils
import com.twitter.util.Future
import com.wisedu.next.api.domains._
import com.wisedu.next.api.filters.H5UserContext._
import com.wisedu.next.api.filters.UserContext._
import com.wisedu.next.api.filters.{H5UserFilters, UserFilters}
import com.wisedu.next.api.services._
import com.wisedu.next.services.{BaseFunctions, CollegeBaseService, StaticBaseService, UserBaseService}


class AuthController extends Controller {

  @Inject
  @Flag("local.doc.root") var filePath: String = _
  @Inject var usersService: UsersService = _
  @Inject var userBaseService: UserBaseService = _
  @Inject var collegeBaseService: CollegeBaseService = _
  @Inject var serviceFunctions: ServiceFunctions = _
  @Inject var aliSMSFcServiceFunction: AliSMSFcServiceFunction = _
  @Inject var staticBaseService: StaticBaseService = _
  @Inject var imUserService:ImUserService =_
  @Inject var campusService:CampusService =_

  // 用户信息更新
  filter[UserFilters].put("/v2/users/update") { request: PutUserInfoRequest =>
    usersService.userInfoUpdate(request).map{
      resp => response.ok.json(resp)
    }
  }

  // 用户信息查询
  get("/v2/users/:user_id") { request: GetUserInfoRequest =>
    usersService.userInfoSearch(request).map{
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
  }

  // 用户注册
  post("/v2/users") { request: PostUsersRequest =>
    usersService.postUserInfo(request).map {
      resp => response.ok.json(resp)
    }
  }

  // 用户社交网络方式注册判断
  post("/v2/users/sns") { request: PostVerifyUsersSnsRequest =>
    usersService.verifyUserSnsInfo(request).map {
      resp => response.ok.json(resp)
    }
  }

  // 用户修改密码
  post("/v2/users/renew/password") { request: PostModifyPasswordReq =>
    usersService.modifyPassword(request).map {
      resp => response.ok.json(resp)
    }
  }


  // 取消绑定社交账号
  put("/v2/users/:user_id/unbind/sns") { request: PutUserUnBindSnsRequest =>
    usersService.unBindUserSns(request).map{
      resp => response.ok.json(resp)
    }
  }

  // 用户注册发送短信验证码 / 修改密码
  post("/v2/users/register/send/msg") { request: PostUserRegSendMsgRequest =>
    aliSMSFcServiceFunction.sendRegSms(request.phone.phone_no,request.method)
  }



  // 用户注册验证短信验证码 / 修改密码
  post("/v2/users/register/verify/authCode") { request: PostUserRegVerifyAuthCodeRequest =>
    aliSMSFcServiceFunction.sendRegVerifySms(request.verify.phone_no, request.verify.auth_code).map {
      item => if (item) {
        PostUserRegVerifyAuthCodeResponse(PostUserRegVerifyResponse(request.verify.phone_no, request.verify.auth_code, "success"))
      } else {
        PostUserRegVerifyAuthCodeResponse(PostUserRegVerifyResponse(request.verify.phone_no, request.verify.auth_code, "fail"))
      }
    }
  }

  // 社交网络登陆认证
  post("/v2/auth/tokens") { request: PostAuthTokensRequest =>
    usersService.postUserSnsAuth(request).map{
      resp => if(resp.status.equals("success")){
        response.ok.json(resp)
      }else{
        response.badRequest.json(resp)
      }

    }
  }

  case class H5User(token: String, userid: UUID, isvisitor: Boolean)

  // H5登录认证
  filter[H5UserFilters].post("/v2/auth/h5/tokens") { request: Request =>
    //val token = new Cookie("token", request.user.tokenS)
    //val userId = new Cookie("userid", request.user.userId.toString)
    //response.ok.cookie(token).cookie(userId).header("Access-Control-Allow-Origin", "*")
    H5User(request.h5User.tokenS, request.h5User.userId, request.h5User.isVisitor)
  }

  // 绑定社交账号
  put("/v2/users/:user_id/bind/sns") { request: PutUserBindSnsRequest =>
    usersService.bindUserSns(request).map{
      resp => response.ok.json(resp)
    }
  }



  // 验证手机唯一
  post("/v2/users/validity/phone") { request: PostUsersPhoneValidityRequest =>
    serviceFunctions.verifyPhone(request.validity.phone_no,request.validity.user_id.getOrElse("")).map {
      verifyInfo =>
        if (verifyInfo)
          GetUsersValidityResponse(GetValidityResp("success"))
        else
          GetUsersValidityResponse(GetValidityResp("fail"))
    }
  }

  // 验证昵称唯一
  post("/v2/users/validity/alias") { request: PostUsersAliasValidityRequest =>
    serviceFunctions.verifyName(request.validity.alias,request.validity.user_id.getOrElse("")).map {
      verifyInfo =>
        if (verifyInfo)
          GetUsersValidityResponse(GetValidityResp("success"))
        else
          GetUsersValidityResponse(GetValidityResp("fail"))
    }
  }

  def postImg(userId: UUID, file: MultipartItem, path: String): Future[Object] = {
    userBaseService.getUserById(userId).flatMap {
      case Some(user) =>
        staticBaseService.putBucket(path).flatMap {
          rst => if (rst) {
          staticBaseService.putObject(path, BaseFunctions.getRandomString(5, file.filename.getOrElse("wuming")), file.data).flatMap {
              filePath =>{
                val mImgF = staticBaseService.compressionUpdateImgs(filePath, 300, 300, "m")
                val lImgF = staticBaseService.compressionUpdateImgs(filePath, 400, 400, "l")
                val sImgF = staticBaseService.compressionUpdateImgs(filePath, 80, 80, "s")
                val ys = for{
                  mImg <- mImgF
                  lImg <- lImgF
                  sImg <- sImgF
                }yield(mImg,lImg,sImg)
                ys.flatMap{
                  case (mImg,lImg,sImg) =>
                    userBaseService.updUserImg(userId, mImg.mkString(",")).map {
                      result =>
                        imUserService.updImUsers(Seq(user.copy(imgUrl = mImg.mkString(","))))
                        Thread.sleep(2000) //预留2000毫秒服务器图片同步时间
                        PostUserImgResponse(userId, mImg.mkString(","))
                    }
                }
              }

            }
          } else {
            Future(PostUserImgResponse(userId, ""))
          }
        }
      case None =>
        response.badRequest.json(Map("errMsg" -> "Current user may not exists!")).toFuture
    }
  }

  // 上传用户头像
  post("/v2/users/:user_id/userImg/upload") { request: PostUserImgRequest =>
    val user_id = request.user_id

    val file = RequestUtils.multiParams(request.request).values.head
    val path = user_id.toString

    postImg(user_id, file, path)
  }

  filter[UserFilters].post("/v2/users/userImg/upload") { request: Request =>
    val userId = request.user.userId

    val file = RequestUtils.multiParams(request).values.head
    val path = userId.toString

    postImg(userId, file, path)
  }



  //初始化用户到IM
  post("/v2/user/init/im") { request: Request =>
    usersService.initUsersToIm().map{
      rsp => response.ok.json(rsp)
    }
  }

  //更新用户到IM
  post("/v2/user/upd/im") { request: Request =>
    usersService.updUsersToIm().map{
      rsp => response.ok.json(rsp)
    }
  }

  //删除用户到IM
  post("/v2/user/del/im") { request: Request =>
    usersService.delUsersToIm().map{
      rsp => response.ok.json(rsp)
    }
  }

  // 获取用户IDS信息
  post("/v2/user/getIDS"){ request: PostIdsRegRequest =>
    campusService.getNjxzUserInfoByToken(request.token).map{
      rsp => response.ok.json(rsp)
    }
  }

  // 获取阿里百川用户
  get("/v2/user/getIm"){ request: Request =>
    imUserService.getImgUsers(request.getParam("userId")).map{
      rsp => response.ok.json(rsp)
    }
  }

  // 查询聊天记录
  get("/v2/user/getImChatLog"){ request: Request =>
    imUserService.getImgUsersChatLogs(request.getParam("userId1"),request.getParam("userId2")).map{
      rsp => response.ok.json(rsp)
    }
  }

  // 群发消息给用户
  post("/v2/user/pushMsg"){ request: PushMsgRequest =>
    usersService.sendMsgToIm(request.from, request.content).map{
      rsp => response.ok.json(rsp)
    }
  }

}

