package com.wisedu.next.api.services

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.UUID
import javax.inject.{Inject, Singleton}

import com.twitter.finatra.httpclient.{HttpClient, RequestBuilder}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.util.Future
import com.wisedu.next.annotations.IdxNjxzHttpClient
import com.wisedu.next.api.domains.{CampusNjxzAuthResp, CampusNjxzUserInfo, CampusNjxzUserInfoResp}
import com.wisedu.next.models.User
import com.wisedu.next.services.{BaseFunctions, CollegeBaseService, StaticBaseService, UserBaseService}
import org.joda.time.DateTime

/**
 * Version: 1.1
 * Author: pattywgm
 * Time: 16/6/28 下午1:44
 * Desc:
 */

@Singleton
class CampusService {

  var key: String = "xzu@wisedu"
  @Inject
  @IdxNjxzHttpClient var idxNjxzHttpClient: HttpClient = _
  @Inject var objectMapper: FinatraObjectMapper = _
  @Inject var collegeBaseService: CollegeBaseService = _
  @Inject var userBaseService: UserBaseService = _
  @Inject var staticBaseService: StaticBaseService = _


  /**
   * Version: 1.1
   * Author: mzp
   * Time:
   * Desc: 根据用户名,密码 登录爱晓庄客户端
   * param: uid 用戶名 , pass 密碼
   */
  def getNjxzTokens(uid: String, pass: String): Future[CampusNjxzAuthResp] = {
    val timeValue = System.currentTimeMillis().toString.substring(0, 10)
    val sign = getSign(uid, pass, key, timeValue)
    val path = "/ids4proxy/login/auth?uid=" + uid + "&pass=" + pass + "&time=" + timeValue + "&sign=" + sign
    try {
      idxNjxzHttpClient.execute(RequestBuilder.post(path)).map {
        response => if (response.contentString == "null") {
          CampusNjxzAuthResp(false, Some("CampusNjxz return error"), Some(""))
        } else {
          objectMapper.parse[CampusNjxzAuthResp](response.contentString)
        }
      }
    } catch {
      case _: Exception => Future(CampusNjxzAuthResp(false, Some("system error"), Some("")))
    }
  }

  /**
   * Version: 1.1
   * Author: mzp
   * Time:
   * Desc: 根据token 获取当前登录用户基本信息
   * param: token 登录标示
   */
  def getNjxzUserInfoByToken(token: String): Future[CampusNjxzUserInfoResp] = {
    val path = "/ids4proxy/query/userInfo?token=" + token
    try {
      idxNjxzHttpClient.execute(RequestBuilder.post(path)).map {
        response => if (response.contentString == "null") {
          CampusNjxzUserInfoResp(false, Some("CampusNjxz return error"), None)
        } else {
          objectMapper.parse[CampusNjxzUserInfoResp](response.contentString)
        }
      }
    } catch {
      case _: Exception => Future(CampusNjxzUserInfoResp(false, Some("system error"), None))
    }
  }

  def getDefaultUserInfo(): CampusNjxzUserInfo = {
    CampusNjxzUserInfo("", "", "", None, "", "", 1, "", "", 0, None)
  }

  /**
   * Version: 1.1
   * Author: mzp
   * Time:
   * Desc: 通过用户名密码获取用户基本信息
   * param: token 登录标示
   */
  def getNjxzUserInfoByAuth(uid: String, pass: String): Future[CampusNjxzUserInfoResp] = {
    val timeValue = System.currentTimeMillis().toString.substring(0, 10)
    val sign = getSign(uid, pass, key, timeValue)
    val path = "/ids4proxy/query/userInfoByLogin?uid=" + uid + "&pass=" + pass + "&time=" + timeValue + "&sign=" + sign
      idxNjxzHttpClient.execute(RequestBuilder.post(path)).map {
        response => if (response.contentString == "null") {
          CampusNjxzUserInfoResp(false, Some("CampusNjxz return error"), None)
        } else {
          objectMapper.parse[CampusNjxzUserInfoResp](response.contentString)
        }
      }.rescue {
        case e: Exception => Future(CampusNjxzUserInfoResp(false,Some(e.toString), None))
      }

  }

  def getSign(uid: String, pass: String, secretKey: String, timeValue: String): String = {
    val md = MessageDigest.getInstance("MD5")
    BaseFunctions.byte2hex(md.digest((secretKey + uid + pass + timeValue).getBytes("utf-8"))).toLowerCase()
  }

  //0  新生   1 老生    2 老师
  def getUserRole(openId: String, userLevel: Int): Int = {
    val lenT = openId.length
    val firstLet = openId.charAt(0)
    //val isTech = firstLet.equals('p') || firstLet.equals('P') || firstLet.equals('z') || firstLet.equals('Z') || lenT.equals(7)
    val isFresh = lenT.equals(14)
    if (userLevel.equals(1)) {
      2
    } else if (isFresh) {
      0
    } else {
      1
    }
  }

  def toUser(campusNjxzUserInfo: CampusNjxzUserInfo, deviceId: String, userId: Option[UUID] = None,
             alias: Option[String] = None): Future[User] = {

    val userRole = getUserRole(campusNjxzUserInfo.WID, campusNjxzUserInfo.SF)
    //cloudUser.getAvatar.getUrl

    val password = ""

    val name = campusNjxzUserInfo.XM

    //cloudUser.getGender
    val sex = campusNjxzUserInfo.XB

    val phoneNo = ""

    val home = ""

    val loveStatus = ""

    val majors = ""

    val cla = ""

    val dorm = ""

    val mschool = ""

    val sign = ""

    val birth_date = new DateTime(new SimpleDateFormat("yyyy-MM-dd").parse(campusNjxzUserInfo.CSRQ))

    val fresh_date = campusNjxzUserInfo.RXRQ match {
      case Some(date) => new DateTime(date).toString("yyyy")
      case None =>
        userRole match {
          case 2 => ""
          case 1 => "20" + campusNjxzUserInfo.WID.substring(0,2)
          case _ => DateTime.now().toString("yyyy")
        }
    }

    val interests = ""

    val actives = ""

    val anonymous = 0

    val backgroundImg = ""

    val user_id = userId.getOrElse(UUID.randomUUID())
    val collegeId = "njxz"
    val collegeNameF = collegeBaseService.getCollegeById(collegeId).map {
      case Some(college) => college.name
      case None => ""
    }

    val imgUrlF = campusNjxzUserInfo.AVATAR_URL match {
      case Some(url) =>
        if (url.nonEmpty) {
          staticBaseService.fetchUrl(url, user_id.toString, UUID.randomUUID().toString)
          //Future( campusNjxzUserInfo.AVATAR_URL)
        } else {
          Future("")
        }
      case None => Future("")
    }


    val departIdF = if (campusNjxzUserInfo.YXID.nonEmpty) {
      collegeBaseService.getDepartsByCollegeIdAndMId(collegeId, campusNjxzUserInfo.YXID).map {
        case Some(dept) => dept.departId
        case None => ""
      }
    } else {
      Future { "" }
    }

    val nickName = userRole match {
      case 0 => campusNjxzUserInfo.XM
      case _ => campusNjxzUserInfo.NC
    }
    val aliasF = alias match {
      case Some(str) => Future { str }
      case None => userBaseService.getUniqueAlice(nickName)
    }

    val descr = "爱晓庄手机号码: " + campusNjxzUserInfo.SJ

    val degreeT = Future { Some(DateTime.now().toString("yyyy").toInt - fresh_date.toInt + 1) }.rescue {
      case e: Exception => Future { None }
    }
    val degreeF = userRole match {
      case 2 => Future { "" }
      case _ => degreeT.map {
        case Some(str) => str.toString
        case None => ""
      }
    }

    for {
      collegeName <- collegeNameF
      alias <- aliasF
      departId <- departIdF
      imgUrl <- imgUrlF
      degree <- degreeF
    } yield User(user_id, alias, name, password, imgUrl, sex, phoneNo, mschool, collegeId, collegeName, departId, cla,
      majors, dorm, degree, fresh_date, birth_date, home, loveStatus, sign, interests, actives,
      DateTime.now, UUID.randomUUID().toString, deviceId, anonymous, 1, backgroundImg, descr, userRole)
  }

  def completeUser(campusNjxzUserInfo: CampusNjxzUserInfo, oldUser: User, deviceId: String): Future[User] = {

    val birth_date = new DateTime(new SimpleDateFormat("yyyy-MM-dd").parse(campusNjxzUserInfo.CSRQ))

    //val fresh_date = campusNjxzUserInfo.RXRQ match {
    //  case Some(date) => new DateTime(date).toString("yyyy")
    //  case None => DateTime.now().toString("yyyy")
    //}

    val imgUrlF = campusNjxzUserInfo.AVATAR_URL match {
      case Some(url) =>
        if (url.nonEmpty && oldUser.imgUrl.isEmpty) {
          staticBaseService.fetchUrl(url, oldUser.userId.toString, UUID.randomUUID().toString)
          //Future( campusNjxzUserInfo.AVATAR_URL)
        } else {
          Future(oldUser.imgUrl)
        }
      case None => Future(oldUser.imgUrl)
    }

    //val departIdF = collegeBaseService.getDepartsByCollegeIdAndMId(oldUser.collegeId, campusNjxzUserInfo.YXID).map {
    //  case Some(dept) => dept.departId
    //  case None => ""
    //}

    val descr = "爱晓庄手机号码: " + campusNjxzUserInfo.SJ

    for {
      //departId <- departIdF
      imgUrl <- imgUrlF
    } yield User(oldUser.userId, oldUser.alias, oldUser.name, oldUser.password, imgUrl, oldUser.sex, oldUser.phoneNo,
      oldUser.mSchool, oldUser.collegeId, oldUser.collegeName, oldUser.depart, oldUser.cla,
      oldUser.majors, oldUser.dorm, oldUser.degree, oldUser.freshDate, birth_date, oldUser.home, oldUser.loveSt, oldUser.sign,
      oldUser.interests, oldUser.actives,
      oldUser.cTime, oldUser.tokenS, deviceId, oldUser.isAnonymousUser, oldUser.status, oldUser.backgroundImg, descr, oldUser.role)
  }

}
