package com.wisedu.next.api.services

import javax.inject.Inject

import com.google.inject.Singleton
import com.twitter.util.Future
import com.wisedu.next.api.domains._
import com.wisedu.next.api.filters.UserContext._
import com.wisedu.next.services.CirclesBaseService

/**
 * Version: 1.1
 * Author: pattywgm
 * Time: 16/7/6 下午3:02
 * Desc:
 */

@Singleton
class CircleService {

  @Inject var circlesBaseService: CirclesBaseService = _
  @Inject var serviceFunctions: ServiceFunctions = _

  //获取圈子公告以及管理员
  def getCircleNotice(request: GetCircleNoticeRequest): Future[GetCircleNoticeResponse] = {
    val collegeId = request.college_id.getOrElse(request.request.user.collegeId)
    val circleF = request.circle_id match {
      case Some(item) => circlesBaseService.getCircleById(item)
      case None => if (request.is_recommend.getOrElse("0").equals("1")) {
        circlesBaseService.getRecommendCircle(collegeId)
      } else {
        circlesBaseService.getCollegeDefaultDisPlayCircle(collegeId)
      }
    }
    circleF.map {
      case Some(circle) => GetCircleNoticeResponse("success", "", Some(CircleNotice(circle.circleId, circle.notice, circle.adminUser)))
      case None => GetCircleNoticeResponse("fail", "circle is not exit", None)
    }
  }


  // 获取圈子列表
  def getCircles(request: GetCirclesRequest): Future[GetCirclesResponse] = {
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)
    val collegeId = request.college_id.getOrElse(request.request.user.collegeId)
    val defaultCircleF = circlesBaseService.getCollegeDefaultSaveCircle(collegeId).map {
      case Some(item) => item.circleId
      case None => ""
    }
    val circlesF = circlesBaseService.collCircles(collegeId, "", "0", "", limits, offset)
    (for {
      circles <- circlesF
      defaultCircle <- defaultCircleF
    } yield (circles, defaultCircle)).map {
      case (circles, defaultCircle) => circles.map {
        circle =>
          GetCirclesResp(s"/v2/${circle.circleId}/msgs", circle.circleId, circle.circleName,
            circle.iconUrl, circle.isAnonymous, circle.isRealNamePost,
            circle.isRealNameRespond, if (circle.circleId.equals(defaultCircle)) 1 else 0, circle.adminUser)
      }
    }.map(item => GetCirclesResponse("success", "", item))
  }

  // 获取推荐圈子列表
  def getCirclesRecommend(request: GetCirclesRequest): Future[GetCirclesResponse] = {
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)
    val collegeId = request.college_id.getOrElse(request.request.user.collegeId)
    val defaultCircleF = circlesBaseService.getCollegeDefaultSaveCircle(collegeId).map {
      case Some(item) => item.circleId
      case None => ""
    }
    val circlesF = circlesBaseService.collCircles(collegeId, "", "0", "", limits, offset)
    (for {
      circles <- circlesF
      defaultCircle <- defaultCircleF
    } yield (circles, defaultCircle)).map {
      case (circles, defaultCircle) => circles.map {
        circle =>
          GetCirclesResp(s"/v2/${circle.circleId}/msgs", circle.circleId, circle.circleName,
            circle.iconUrl, circle.isAnonymous, circle.isRealNamePost,
            circle.isRealNameRespond, if (circle.circleId.equals(defaultCircle)) 1 else 0, circle.adminUser)
      }
    }.map(item => GetCirclesResponse("success", "", item))
  }

}
