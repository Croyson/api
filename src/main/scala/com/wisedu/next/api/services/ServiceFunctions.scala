package com.wisedu.next.api.services

import java.util.UUID
import java.util.regex.Pattern
import javax.inject.{Inject, Singleton}
import com.twitter.finatra.utils.FuturePools
import com.twitter.util.Future
import com.wisedu.next.api.domains._
import com.wisedu.next.models._
import com.wisedu.next.services._
import org.joda.time.DateTime
import dispatch._, Defaults._
@Singleton
class ServiceFunctions {

  @Inject var collegeBaseService: CollegeBaseService = _
  @Inject var tagBaseService: TagBaseService = _
  @Inject var topicBaseService: GroupBaseService = _
  @Inject var userBaseService: UserBaseService = _
  @Inject var baseService: BaseService = _
  @Inject var staticBaseService: StaticBaseService = _
  private val futurePool = FuturePools.unboundedPool("CallbackConverter")

  def toUser(postUsersRequest: PostUsersRequest, isAnonymous: Boolean): Future[User] = {


    val password = postUsersRequest.user.password match {
      case Some(item) => BaseFunctions.md5Encryption(item, "cpdaily123")
      case None => ""
    }

    val name = postUsersRequest.user.name match {
      case Some(item) => item
      case None => ""
    }

    val sex = postUsersRequest.user.sex match {
      case Some(item) => item
      case None => 0
    }

    val phoneNo = postUsersRequest.user.phone_no match {
      case Some(item) => item
      case None => ""
    }

    val home = postUsersRequest.user.home match {
      case Some(item) => item
      case None => ""
    }

    val loveStatus = postUsersRequest.user.love_status match {
      case Some(item) => item
      case None => ""
    }

    val depart = postUsersRequest.user.depart match {
      case Some(item) => item
      case None => ""
    }

    val majors = postUsersRequest.user.majors match {
      case Some(item) => item.mkString(",")
      case None => ""
    }

    val cla = postUsersRequest.user.cla match {
      case Some(item) => item
      case None => ""
    }

    val dorm = postUsersRequest.user.dorm match {
      case Some(item) => item
      case None => ""
    }

    val mschool = postUsersRequest.user.mschool match {
      case Some(item) => item
      case None => ""
    }

    val sign = postUsersRequest.user.sign match {
      case Some(item) => item
      case None => ""
    }

    val birth_date = postUsersRequest.user.birth_date match {
      case Some(item) => item
      case None => new DateTime("1983-06-30T01:20+02:00")
    }

    val fresh_date = postUsersRequest.user.fresh_date match {
      case Some(item) => item
      case None => ""
    }

    val interests = postUsersRequest.user.interests match {
      case Some(item) => item.mkString(",")
      case None => ""
    }

    val actives = postUsersRequest.user.actives match {
      case Some(item) => item.mkString(",")
      case None => ""
    }

    val deviceId = postUsersRequest.user.device_id

    var anonymous = 0
    if (isAnonymous) {
      anonymous = 1
    }

    val user_id = UUID.randomUUID()
    val collegeNameF = collegeBaseService.getCollegeById(postUsersRequest.user.college).map {
      case Some(college) => college.name
      case None => ""
    }

    val aliasF = if (isAnonymous) {
      userBaseService.getUniqueAlice("匿名" + DateTime.now().toString("yyyyMMddHHmmss"))
    } else {
      userBaseService.getUniqueAlice(postUsersRequest.user.alias.getOrElse(""))
    }

    val backgroundImg = postUsersRequest.user.backgroundImg match {
      case Some(item) => item
      case None => ""
    }

    val imgUrlF = postUsersRequest.user.img_url match {
      case Some(item) => if (item.indexOf("http") > -1) {
        staticBaseService.fetchUrl(item, user_id.toString, UUID.randomUUID().toString)
      } else {
        Future(item)
      }
      case None => Future("")
    }

    for {
      collegeName <- collegeNameF
      alias <- aliasF
      imgUrl <- imgUrlF
    } yield User(user_id, alias, name, password, imgUrl, sex, phoneNo, mschool, postUsersRequest.user.college, collegeName, depart, cla,
      majors, dorm, "三年级", fresh_date, birth_date, home, loveStatus, sign, interests, actives,
      DateTime.now, UUID.randomUUID().toString, deviceId, anonymous, 1, backgroundImg, "",1)

  }

  def toUserSnsInfo(userId: UUID, postUsersRequest: PostUsersRequest): UserSnsInfo = {

    val openId = postUsersRequest.user.openid match {
      case Some(item) => item
      case None => ""
    }

    val snsCode = postUsersRequest.user.sns_code match {
      case Some(item) => item
      case None => ""
    }

    val snsType = postUsersRequest.user.sns_type match {
      case Some(item) => item
      case None => 0
    }

    val snsAlias = postUsersRequest.user.alias match {
      case Some(item) => item
      case None => ""
    }

    UserSnsInfo(userId, openId, snsCode, snsAlias, snsType, DateTime.now())
  }

  def toPostUsersResponse(method: String, user: User, userSnsInfo: Option[UserSnsInfo]): Future[PostUsersResponse] = {
    val postSnsResps: Set[PostSnsResp] = userSnsInfo match {
      case Some(item) => Set(PostSnsResp(item.openId, item.snsType, item.snsCode, item.snsAlias))
      case None => Set()
    }
    val collegeShortNameF = baseService.getCollegeShortNameByID(user.collegeId)
    val depF = collegeBaseService.getDepartById(user.depart).map {
      case Some(item) => (item.departName, item.departShortName)
      case None => ("", "")
    }
    for {
      collegeShortName <- collegeShortNameF
      dep <- depF
    } yield
    PostUsersResponse("success", "", method, Some(PostUserResp(user.userId, s"/v2/users/${user.userId}", Some(user.imgUrl), Some(user.alias), None,
      Some(user.name), Some(user.sex), Some(user.collegeId), Some(user.collegeName), Some(user.phoneNo), Some(user.home),
      Some(user.loveSt), Some(dep._1), Some(user.majors), Some(user.cla), Some(user.dorm), Some(user.degree),
      Some(user.mSchool), Some(user.sign), Some(user.birthDate), Some(user.freshDate), Some(user.interests),
      Some(user.actives), Some(user.cTime),Some(user.tokenS),Some(user.deviceId), postSnsResps, Some(collegeShortName),
      Some(user.backgroundImg), Some(dep._2),Some(user.role))))
  }


  def toPostUsersResponse(method: String, msg: String, user: User, userSnsInfos: Seq[UserSnsInfo], alias: String): Future[PostUsersResponse] = {
    val postSnsResps = userSnsInfos.map{
      userSnsInfo => PostSnsResp(userSnsInfo.openId,userSnsInfo.snsType,userSnsInfo.snsCode,userSnsInfo.snsAlias)
    }.toSet
    val collegeShortNameF = baseService.getCollegeShortNameByID(user.collegeId)
    val depF = collegeBaseService.getDepartById(user.depart).map {
      case Some(item) => (item.departName, item.departShortName)
      case None => ("", "")
    }
    for {
      collegeShortName <- collegeShortNameF
      dep <- depF
    } yield
    PostUsersResponse("success", msg, method, Some(PostUserResp(user.userId, s"/v2/users/${user.userId}", Some(user.imgUrl), Some(alias), None,
      Some(user.name), Some(user.sex), Some(user.collegeId), Some(user.collegeName), Some(user.phoneNo), Some(user.home),
      Some(user.loveSt), Some(dep._1), Some(user.majors), Some(user.cla), Some(user.dorm), Some(user.degree),
      Some(user.mSchool), Some(user.sign), Some(user.birthDate), Some(user.freshDate), Some(user.interests),
      Some(user.actives), Some(user.cTime),Some(user.tokenS),Some(user.deviceId), postSnsResps, Some(collegeShortName),
      Some(user.backgroundImg), Some(dep._2),Some(user.role))))

  }

  def toGetUserResponse(user: User): Future[GetUserResponse] = {
    val collegeShortNameF = baseService.getCollegeShortNameByID(user.collegeId)
    val depF = collegeBaseService.getDepartById(user.depart).map {
      case Some(item) => (item.departName, item.departShortName)
      case None => ("", "")
    }

    val postSnsRespsF = userBaseService.getSnsByUserId(user.userId).map {
      items => items.map {
        item => PostSnsResp(item.openId, item.snsType, item.snsCode, item.snsAlias)
      }.toSet
    }

    for {
      collegeShortName <- collegeShortNameF
      dep <- depF
      postSnsResps <- postSnsRespsF
    } yield GetUserResponse("success", "", Some(GetUserResp(user.userId, s"/v2/users/${user.userId}", Some(user.imgUrl), Some(user.alias), None,
      Some(user.name), Some(user.sex), Some(user.collegeId), Some(user.collegeName), Some(user.phoneNo), Some(user.home),
      Some(user.loveSt), Some(dep._1), Some(user.depart), Some(user.majors), Some(user.cla), Some(user.dorm), Some(user.degree),
      Some(user.mSchool), Some(user.sign), Some(user.birthDate), Some(user.freshDate), Some(user.interests),
      Some(user.actives), Some(user.cTime), Some(user.tokenS),Some(user.deviceId), postSnsResps, Some(collegeShortName),
      Some(user.backgroundImg), Some(dep._2),Some(user.role))))

  }

  def getModifyUserInfo(userId:UUID,userInfoReq: PutUserInfoRequest, oldUser: User): Future[User] = {

    val alias = userInfoReq.user.alias match {
      case Some(item) => item
      case None => oldUser.alias
    }

    val imgUrl = userInfoReq.user.img_url match {
      case Some(item) => item
      case None => oldUser.imgUrl
    }

    val password = userInfoReq.user.password match {
      case Some(item) => item
      case None => oldUser.password
    }

    val name = userInfoReq.user.name match {
      case Some(item) => item
      case None => oldUser.name
    }

    val sex = userInfoReq.user.sex match {
      case Some(item) => item
      case None => oldUser.sex
    }

    val phoneNo = userInfoReq.user.phone_no match {
      case Some(item) => item
      case None => oldUser.phoneNo
    }

    val home = userInfoReq.user.home match {
      case Some(item) => item
      case None => oldUser.home
    }

    val loveStatus = userInfoReq.user.love_status match {
      case Some(item) => item
      case None => oldUser.loveSt
    }

    var depart = userInfoReq.user.depart match {
      case Some(item) => item
      case None => oldUser.depart
    }

    depart = if (userInfoReq.user.college.nonEmpty && !userInfoReq.user.college.getOrElse("").equals(oldUser.collegeId)) "" else depart

    val majors = userInfoReq.user.majors.getOrElse(Set()).mkString(",")

    val cla = userInfoReq.user.cla match {
      case Some(item) => item
      case None => oldUser.cla
    }

    val dorm = userInfoReq.user.dorm match {
      case Some(item) => item
      case None => oldUser.dorm
    }

    val mSchool = userInfoReq.user.mschool match {
      case Some(item) => item
      case None => oldUser.mSchool
    }

    val sign = userInfoReq.user.sign match {
      case Some(item) => item
      case None => oldUser.sign
    }

    val birthDate = userInfoReq.user.birth_date match {
      case Some(item) => item
      case None => oldUser.birthDate
    }

    val freshDate = userInfoReq.user.fresh_date match {
      case Some(item) => item
      case None => oldUser.freshDate
    }

    val interests = userInfoReq.user.interests.getOrElse(Set()).mkString(",")

    val actives = userInfoReq.user.actives.getOrElse(Set()).mkString(",")

    val college = userInfoReq.user.college match {
      case Some(item) => collegeBaseService.getCollegeById(item).map {
        case Some(collegeT) => (item, collegeT.name)
        case None => (item, "")
      }
      case None => Future {
        (oldUser.collegeId, oldUser.collegeName)
      }
    }

    val backgroundImg = userInfoReq.user.backgroundImg match {
      case Some(item) => item
      case None => oldUser.backgroundImg
    }


    college.map {
      case (collegeId, collegeName) =>
        User(userId, alias, name, password, imgUrl, sex, phoneNo, mSchool, collegeId, collegeName, depart, cla,
          majors, dorm, oldUser.degree, freshDate, birthDate, home, loveStatus, sign, interests, actives,
          oldUser.cTime, oldUser.tokenS, oldUser.deviceId, oldUser.isAnonymousUser, 1, backgroundImg, oldUser.descr,1)
    }
  }


  def varifyTag(tagName: String): Future[Boolean] = {
    tagBaseService.getTagByName(tagName).map {
      case Some(item) => true
      case None => false
    }
  }

  def toGetCollegesResponse(colleges: Seq[College]): GetCollegesResponse = {
    GetCollegesResponse(colleges.map {
      case (college) => GetCollegeResp(s"/v2/colleges/${college.collegeId}", college.collegeId,
        college.name, college.eName, college.shortName, college.imgUrl, college.userNum,
        college.region, GetCollegeGpsResp(college.lat, college.lng))
    })
  }

  def toGetDepartInfoResp(departs: Seq[Depart]): GetDepartInfoResp = {
    GetDepartInfoResp(departs.map {
      case (depart) => DepartInfo(depart.departId, depart.collegeId, depart.departName, depart.descr, depart.sortNo)
    })
  }


  def toGetMediasResponse(media: Service): GetMediaResp = {
    GetMediaResp(s"/v2/medias/${media.serId}",
      media.name, media.depict, media.imgUrl, media.mTime)
  }

  def toGetCollegeMediasResponse(media: Service, follow: String): GetCollegeMediaResp = {
    GetCollegeMediaResp(s"/v2/medias/${media.serId}",
      media.name, media.depict, media.imgUrl, media.mTime, follow)
  }

  def toGetMediaInfo(status: String, errMsg: String, media: Service, follow: String, college: String, feedsCount: Int, followCount: Int): GetMediaResponse = {
    GetMediaResponse(status, errMsg, media.name, media.orgType.toString, media.srcType.toString, media.depict, media.imgUrl, media.backImgUrl,
      media.cTime, media.mTime, follow, feedsCount, followCount, college)
  }

  def toGetFeedsBySerIdResp(feed: SqlFeed, feedStats: FeedStat, media: Service): GetMediaFeedResp = {
    GetMediaFeedResp(s"/v2/feeds/${feed.feedId}", feed.title, feed.cTime, feed.mTime, feedStats.readNum.toInt,
      feedStats.updateNum.toInt, feedStats.likeNum.toInt, feed.viewStyle.toInt, media.name, media.imgUrl, feed.summ, feed.sImgUrl,
      feed.resUrl.split(",").length, feedStats.onlineNum.toInt, feed.liveStatus, feed.liveStartTime, feed.contUrl)
  }

  def toUserService(user_id: UUID, media_id: String): UserService = {
    UserService(user_id, media_id, DateTime.now())
  }

  def verifyPhone(phoneNo: String, userId: String): Future[Boolean] = {
    val reg = "\\d{11}"
    if (Pattern.matches(reg, phoneNo)) {
      userBaseService.getUserByPhoneNo(phoneNo).map {
        case Some(user) =>
          if (userId.isEmpty || (userId.nonEmpty && !userId.equals(user.userId.toString)))
            false
          else
            true
        case None => true
      }
    } else {
      Future(false)
    }
  }

  def verifyName(alias: String, userId: String): Future[Boolean] = {
    userBaseService.getUserByAlias(alias).map {
      case Some(user) =>
        if (userId.isEmpty || (userId.nonEmpty && !userId.equals(user.userId.toString)))
          false
        else
          true
      case None => true
    }
  }

  def verifyPhoneInfo(phoneNo: Option[String], password: Option[String]): Future[(Option[User],String)] = {
    val phone_s = phoneNo.getOrElse("")
    val password_s = password.getOrElse("")
    if (phone_s.isEmpty || password_s.isEmpty) {
      Future {
        (None,"param is error")
      }
    } else {
      userBaseService.getUserByPhoneNo(phone_s).map {
        case Some(user) => if (user.password.equals(BaseFunctions.md5Encryption(password_s, "cpdaily123")) ||
          user.password.equals(password_s)) (Some(user),"")
        else (None,"password error")
        case None => (None,"phone is not exit")
      }
    }
  }

  def verifySnsInfo(openId: Option[String], snsType: Option[Int]): Future[Option[UserSnsInfo]] = {
    val openId_s = openId.getOrElse("")
    val snsType_s = snsType.getOrElse(0)
    if (openId_s.isEmpty || snsType_s.equals(0)) {
      Future {
        None
      }
    } else {
      userBaseService.getSnsByOpenId(openId.get, snsType.get).map {
        case Some(user) => Some(user)
        case None => None
      }
    }
  }

  def toFeedLike(feed_id: UUID, user_id: UUID, op_type: Int, op_msg: String): FeedLike = {
    FeedLike(feed_id, user_id, op_type, DateTime.now, op_msg)
  }

  def toFeedCollect(feed_id: UUID, user_id: UUID): FeedCollect = {
    FeedCollect(feed_id, user_id, DateTime.now)
  }

  def toFeedShare(feed_id: UUID, user_id: UUID, share_type: String, share_url: String, src_value: String): FeedShare = {
    FeedShare(UUID.fromString(src_value), feed_id, user_id, DateTime.now, share_type.toInt, share_url)
  }

  def toUpdateLike(user_id: UUID, update_id: UUID, op_type: Int): UpdateLike = {
    UpdateLike(update_id, user_id, op_type, DateTime.now)
  }

  def toFeedReadStat(readStat: FeedReadStat): FeedReadStat = {
    FeedReadStat(readStat.feedId, readStat.userId, readStat.frTime, DateTime.now, readStat.readCount + 1, "")
  }

  def toFeedReadStat(feed_id: UUID, user_id: String): FeedReadStat = {
    FeedReadStat(feed_id, user_id, DateTime.now, DateTime.now, 1, "")
  }

  def toFeedReadDetail(feed_id: UUID, user_id: String, read_type: Int, src_value: String, push_value: String, ip_addr: String): FeedReadDetail = {
    FeedReadDetail(UUID.randomUUID(), feed_id, user_id, read_type, DateTime.now, src_value, push_value, ip_addr)
  }


  def toUpdate(method: Int, feed_id: UUID, pupdate_id: String, user_id: UUID, content: String, img_urls: String, is_anonymous: Int, update_type: Int, thresh_hold: Int, fuzzyImgs: String): FeedUpdate = {
    if (method == 1) {
      FeedUpdate(UUID.randomUUID(), feed_id, user_id, content, DateTime.now, 0L, 0L, UUID.fromString(pupdate_id), 1, img_urls, 0L, is_anonymous, update_type, thresh_hold, fuzzyImgs, 0)
    }
    else {
      FeedUpdate(UUID.randomUUID(), feed_id, user_id, content, DateTime.now, 0L, 0L, feed_id, 0, img_urls, 0L, is_anonymous, update_type, thresh_hold, fuzzyImgs, 0)
    }
  }

  def toGetUpdatesTopResp(update: FeedUpdate, user: User, is_like: String, is_emotion: String,isRealNamePost:Int): GetUpdatesTopResp = {
    if (update.isAnonymous == 1)
      GetUpdatesTopResp(s"/v2/updates/${update.updateId.toString}", "匿名用户", "/v2/statics/anonymousUser.jpg", s"/v2/users/anonymousUserPage",
        "","","", "", "", update.likeNum, update.content, "1", DateTime.now, is_like, is_emotion, "1", user.userId.toString)
    else
      GetUpdatesTopResp(s"/v2/updates/${update.updateId.toString}",   if (isRealNamePost == 1 && user.name.nonEmpty) user.name else user.alias,user.name,user.freshDate, user.imgUrl, s"/v2/users/${user.userId}",
        user.collegeName, user.depart, user.cla, update.likeNum, update.content, user.sex.toString, DateTime.now, is_like, is_emotion, "0", user.userId.toString)
  }

  def toGetUpdatesResp(feed: Feed, update: FeedUpdate, user: User,
                       sub_update_num: Long, depart: String, like: String,
                       is_emotion: String, emotions: Seq[GetEmotionsResp],
                       like_num: Long, like_users: Seq[GetEmotionsUserResp],
                       collegeShortName: String,isRealNamePost:Int,
                       replyMsgId:String,replyUserId:String,replyUserName:String,
                       replyUserRealName:String): GetUpdatesResp = {
    if (feed.topicInviter.isEmpty) {
      if (update.isAnonymous == 1)
        GetUpdatesResp(s"/v2/updates/${update.updateId.toString}", "", "匿名用户", "/v2/statics/anonymousUser.jpg", s"/v2/users/anonymousUserPage",
          "", "", "","","", "", update.likeNum, update.content, "0", update.cTime,
          sub_update_num, update.imgUrls, like, is_emotion, emotions, like_users, like_num, "1", update.updateType, update.threshHold,
          replyMsgId,replyUserId,if (isRealNamePost == 1 && replyUserRealName.nonEmpty)replyUserRealName else replyUserName)
      else
        GetUpdatesResp(s"/v2/updates/${update.updateId.toString}", user.userId.toString,  if (isRealNamePost == 1 && user.name.nonEmpty) user.name else user.alias,user.name,user.freshDate, user.imgUrl, s"/v2/users/${user.userId}",
          user.collegeName, collegeShortName, depart, user.cla, update.likeNum, update.content, user.sex.toString, update.cTime,
          sub_update_num, update.imgUrls, like, is_emotion, emotions, like_users, like_num, "0", update.updateType, update.threshHold,
          replyMsgId,replyUserId,if (isRealNamePost == 1 && replyUserRealName.nonEmpty)replyUserRealName else replyUserName)
    }
    else {
      val img = if (update.likeNum >= update.threshHold) update.imgUrls else update.fuzzyImgs
      if (update.isAnonymous == 1)
        GetUpdatesResp(s"/v2/updates/${update.updateId.toString}", "", "匿名用户", "/v2/statics/anonymousUser.jpg", s"/v2/users/anonymousUserPage",
          "", "", "","","","", update.likeNum, update.content, " ", update.cTime,
          sub_update_num, img, like, is_emotion, emotions, like_users, like_num, "1", update.updateType, update.threshHold,
          replyMsgId,replyUserId,if (isRealNamePost == 1 && replyUserRealName.nonEmpty)replyUserRealName else replyUserName)
      else
        GetUpdatesResp(s"/v2/updates/${update.updateId.toString}", user.userId.toString,  if (isRealNamePost == 1 && user.name.nonEmpty) user.name else user.alias,user.name,user.freshDate, user.imgUrl, s"/v2/users/${user.userId}",
          user.collegeName, collegeShortName, depart, user.cla, update.likeNum, update.content, user.sex.toString, update.cTime,
          sub_update_num, img, like, is_emotion, emotions, like_users, like_num, "0", update.updateType, update.threshHold,
          replyMsgId,replyUserId,if (isRealNamePost == 1 && replyUserRealName.nonEmpty)replyUserRealName else replyUserName)
    }

  }

  //时间顺序排序
  def comp_messageInfoCTime_asc(f1: MessageInfo, f2:  MessageInfo) = {
    if(f1.cTime.isAfter(f2.cTime)) false else true
  }


  //时间顺序排序
  def comp_UserMsgNoticeOption_desc(f1: UserMsgNotice, f2:  UserMsgNotice) = {
    if(f1.opTime.isAfter(f2.opTime)) true else false
  }

  def toGetUpdatesResp(update: FeedUpdate, user: User, sub_update_num: Long, depart: String,
                       like: String, is_emotion: String, emotions: Seq[GetEmotionsResp],
                       like_num: Long, like_users: Seq[GetEmotionsUserResp],
                       collegeShortName: String,isRealNamePost:Int,
                       replyMsgId:String,replyUserId:String,replyUserName:String,
                       replyUserRealName:String): GetUpdatesResp = {

    if (update.isAnonymous == 1)
      GetUpdatesResp(s"/v2/updates/${update.updateId.toString}", "", "匿名用户", "/v2/statics/anonymousUser.jpg", s"/v2/users/anonymousUserPage",
        " ", " ", " ","","", "", update.likeNum, update.content, " ", update.cTime,
        sub_update_num, update.imgUrls, like, is_emotion, emotions, like_users, like_num, "1",
        update.updateType, update.threshHold,
        replyMsgId,replyUserId,if (isRealNamePost == 1 && replyUserRealName.nonEmpty)replyUserRealName else replyUserName)
    else
      GetUpdatesResp(s"/v2/updates/${update.updateId.toString}", user.userId.toString, if (isRealNamePost == 1 && user.name.nonEmpty) user.name else user.alias,user.name,user.freshDate, user.imgUrl, s"/v2/users/${user.userId}",
        user.collegeName, collegeShortName, depart, user.cla, update.likeNum, update.content, user.sex.toString, update.cTime,
        sub_update_num, update.imgUrls, like, is_emotion, emotions,
        like_users, like_num, "0", update.updateType, update.threshHold,
        replyMsgId,replyUserId,if (isRealNamePost == 1 && replyUserRealName.nonEmpty)replyUserRealName else replyUserName)
  }

  def toGetChannelRsp(group: Group): GetChannelRsp = {
    GetChannelRsp(s"/v2/channels/${group.groupId.toString}", group.groupName, group.description, group.backImgUrl, group.sortNo, group.groupType)
  }


  def toGetChannelFeedResp(feedAndStats: FeedAndStats, media_name: String, media_img_url: String): GetChannelFeedResp = {
    GetChannelFeedResp(s"/v2/feeds/${feedAndStats.feedId}", feedAndStats.title, feedAndStats.cTime, feedAndStats.mTime, feedAndStats.readNum,
      feedAndStats.updateNum, feedAndStats.likeNum, feedAndStats.viewStyle.toInt, media_name, media_img_url, feedAndStats.summ,
      feedAndStats.sImgUrl, feedAndStats.sImgUrl.split(",").length, feedAndStats.onlineNum, feedAndStats.liveStatus, feedAndStats.liveStartTime,
      feedAndStats.srcUrl, feedAndStats.videoLength, feedAndStats.videoAddr, feedAndStats.videoType.toString, feedAndStats.contUrl, feedAndStats.advImg, feedAndStats.listImgType)
  }


  def toGetPrizeTopicResp(lotteryDraw: FeedLotteryDraw, title: String, sImg: String): GetPrizeTopicResp = {
    val img = sImg.replaceAll(",", "").stripLineEnd
    GetPrizeTopicResp(UUID.fromString(lotteryDraw.feedId), title, lotteryDraw.lotteryDrawUrl,
      lotteryDraw.lotteryDrawRstUrl, lotteryDraw.finishTime, img)
  }

  def toUpdate(feedUpdate: FeedUpdate, user_name: String, user_img: String, user_sex: String, user_id: String): Update = {
    val img = if (feedUpdate.updateType != 1 && feedUpdate.likeNum < feedUpdate.threshHold) feedUpdate.fuzzyImgs else feedUpdate.imgUrls
    if (feedUpdate.isAnonymous == 1)
      Update("/v2/statics/anonymousUser.jpg", feedUpdate.likeNum, "匿名用户", img, feedUpdate.content, user_sex, "1", feedUpdate.updateType, user_id)
    else
      Update(user_img, feedUpdate.likeNum, user_name, img, feedUpdate.content, user_sex, "0", feedUpdate.updateType, user_id)
  }


  def toGetTopicResponse(feed: Feed, user: User, feedStat: FeedStat, lottery: FeedLotteryDraw, inviters: Seq[GetInvitersResp], isEmotion: String): GetTopicResponse = {
    GetTopicResponse(feed.title, user.alias, user.userId.toString, user.imgUrl, feed.summ, feedStat.readNum,
      feedStat.updateNum, feed.sImgUrl, feed.contUrl, lottery.lotteryDrawUrl, lottery.lotteryDrawRstUrl,
      lottery.finishTime, lottery.lotteryDrawTitle, inviters, isEmotion)
  }

  def toGetTopicResponse(feed: Feed, user: Service, feedStat: FeedStat, lottery: FeedLotteryDraw, inviters: Seq[GetInvitersResp], isEmotion: String): GetTopicResponse = {
    GetTopicResponse(feed.title, user.name, user.serId, user.imgUrl, feed.summ, feedStat.readNum, feedStat.updateNum,
      feed.sImgUrl, feed.contUrl, lottery.lotteryDrawUrl, lottery.lotteryDrawRstUrl,
      lottery.finishTime, lottery.lotteryDrawTitle, inviters, isEmotion)
  }

  def toTopic(feed: FeedAndStats, update: Seq[Update], inviters: Seq[GetInvitersResp]): Topic = {
    Topic(feed.feedId, feed.title, feed.readNum, feed.updateNum, s"/v2/feeds/${feed.feedId.toString}", update, inviters)
  }

  def toGetFeedUpdateResponse(user: User, update: FeedUpdate, is_like: String, is_emotion: String, emotions: Seq[GetEmotionsResp], like_users: Seq[GetEmotionsUserResp], college: String, shortName: String, depart: String,isRealNamePost:Int): GetFeedUpdateResponse = {
    val img = if (update.updateType != 1 && update.likeNum < update.threshHold) update.fuzzyImgs else update.imgUrls
    // 匿名不显示用户ID
    if (update.isAnonymous == 1)
      GetFeedUpdateResponse(GetFeedUpdateResp(update.updateId, update.feedId, UUID.fromString("b4bb03d1-2368-11e6-8a06-acbc327c3dc9"), "匿名用户", "", "","","", "", "", "", update.content, update.cTime,
        update.likeNum, update.unLikeNum, update.pUpdateId, update.updateLevel, img, is_like, is_emotion, emotions, like_users, "1", update.updateType, update.threshHold))
    else
      GetFeedUpdateResponse(GetFeedUpdateResp(update.updateId, update.feedId, update.userId, if (isRealNamePost == 1 && user.name.nonEmpty) user.name else user.alias,user.name,user.freshDate, user.imgUrl, college, shortName, depart, user.sex.toString, update.content, update.cTime,
        update.likeNum, update.unLikeNum, update.pUpdateId, update.updateLevel, img, is_like, is_emotion, emotions, like_users, "0", update.updateType, update.threshHold))
  }


  def toGetMsgInfoResponse(msg: MessageInfo, referMsgInfo: GetReferMsgResp, msgPostUser: GetMsgPosterResp,
                           emotions: Seq[GetEmotionsResp], likeUser: Seq[GetEmotionsUserResp],
                           isLike: String, isEmotion: String,circleName: String,circleIcon:String,
                           group_id: String,isAttention:Int,adminUser:String): GetMsgInfoResponse = {
    val img = if (msg.updateType == 3 && msg.likeNum < msg.threshHold) msg.fuzzyImgs else msg.imgUrls
    GetMsgInfoResponse("success", "", Some(GetMsgInfoResp(msg.messageId.toString, msg.content, img, msg.cTime,
      circleName, group_id,circleIcon, msg.likeNum, msg.unLikeNum, msg.updateNum, msg.messageType.toString, isLike, isEmotion, referMsgInfo, msgPostUser,
      emotions, likeUser, msg.updateType, msg.threshHold, msg.isAnonymous,isAttention,msg.isRecommend,adminUser,msg.isDelete)))

  }
  case class Location(city: String, state: String)

  def weatherSvc(loc: Location) = {
    host("api.wunderground.com") / "api" / "5a7c66db0ba0323a" /
      "conditions" / "q" / loc.state / (loc.city + ".xml")
  }

  def weatherXml(loc: Location) =
    Http(weatherSvc(loc) OK as.xml.Elem)

  def printer = new scala.xml.PrettyPrinter(90, 2)

  def extractTemp(xml: scala.xml.Elem) = {
    val seq = for {
      elem <- xml \\ "temp_c"
    } yield elem.text.toFloat
    seq.head
  }
  def temperature(loc: Location) =
    for (xml <- weatherXml(loc))
      yield extractTemp(xml)
  def testDis():Future[Any]={

    futurePool{
      val locs = List(Location("New York", "NY"),
        Location("Los Angeles", "CA"),
        Location("Chicago", "IL"))
      val temps =
        for(loc <- locs)
          yield for (t <- temperature(loc))
            yield (t -> loc)
    }
  }




}