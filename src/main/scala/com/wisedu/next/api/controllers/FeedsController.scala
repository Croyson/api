package com.wisedu.next.api.controllers

import javax.inject.Inject

import com.twitter.finatra.http.Controller
import com.wisedu.next.api.domains._
import com.wisedu.next.api.filters.UserContext._
import com.wisedu.next.api.services.FeedsService
import com.wisedu.next.services.FeedBaseService


class FeedsController extends Controller {

  @Inject var feedService: FeedsService = _
  @Inject var feedBaseService: FeedBaseService = _

  // 内容分享
  put("/v2/feeds/:feed_id/share") { request: PutFeedShareRequest =>
    feedService.feedShare(request).map {
      resp =>
        if (resp) response.ok.json(GetJsonResponse("success", ""))
        else response.ok.json(GetJsonResponse("fail", ""))
    }
  }

  // 内容喜欢
  put("/v2/feeds/:feed_id/like") { request: PutFeedLikeRequest =>
    feedService.feedLike(request).map {
      resp =>
        if (resp) response.ok.json(GetJsonResponse("success", ""))
        else response.ok.json(GetJsonResponse("fail", ""))
    }
  }

  // 内容收藏
  put("/v2/feeds/:feed_id/collect") { request: PutFeedCollectRequest =>
    feedService.feedCollect(request).map {
      resp =>
        if (resp) response.ok.json(GetJsonResponse("success", ""))
        else response.ok.json(GetJsonResponse("fail", ""))
    }
  }

  // 获取频道咨讯集合
  get("/v2/channels/:channel_id") { request: GetChannelRequest =>
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)
    val channel_id = request.channel_id
    val user_id = request.request.user.userId
    feedService.getChannelFeeds(channel_id, user_id.toString, limits, offset).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
  }


  // 直播频道
  get("/v2/channels/live") { request: GetLiveRequest =>
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)
    val user_id = request.request.user.userId
    feedService.getChannelFeeds("zbpd", user_id.toString, limits, offset).map {
      resp => response.ok.json(resp)
    }
  }

  // 获取频道广告位资讯集合
  get("/v2/channels/:channel_id/posters") { request: GetChannelRequest =>
    val channel_id = request.channel_id
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)

    feedService.getPosterFeeds(channel_id, limits, offset).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
  }

  // 获取圈子广告位资讯集合
  get("/v2/circles/:circle_id/posters") { request:GetCirclePosterRequest  =>
    val circleId = request.circle_id
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)

    feedService.getPosterFeeds(circleId, limits, offset).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
  }


  //获取资讯详情
  get("/v2/feeds/:feed_id") { request: GetFeedInfoRequest =>
    val feed_id = request.feed_id
    val user_id = request.request.user.userId
    val read_type = request.read_type.toInt
    val src_value = request.src_value.getOrElse("")
    val push_value = request.push_value.getOrElse("")
    val ip_addr = request.ip_addr.getOrElse("")

    feedBaseService.getFeedById(feed_id).flatMap {
      case Some(feed) =>
        feedService.getFeedInfos(feed, feed_id, user_id, read_type, src_value, push_value, ip_addr).map {
          case (userService, feedStat, group, updateType, feedLike, lottery, inviters, media,isEmotion) =>
            response.ok.json(GetSimpleFeedInfoResponse(feed.feedId, media._1, media._2, media._3, media._4, userService, s"/v2/medias/${media._1}",
              feed.title, feed.viewStyle, feed.cTime, feed.mTime, feed.srcUrl, feedLike.opType.toString, feed.summ, feed.sImgUrl,
              feed.videoAddr, feed.videoType.toString, feed.contUrl, feed.contentType, updateType.description,
              feedStat.readNum, feedStat.likeNum, feedStat.updateNum, group._1, group._2, group._3, feed.permitEmotions, feed.permitThumbsUp,isEmotion))
        }
      case None => response.badRequest.json(Map("errMsg" -> "The feed does not exist!")).toFuture
    }
  }

  // 用户反馈
  post("/v2/feedbacks") { request: PostFeedbackRequest =>
    feedService.userFeedBack(request).map {
      resp =>
        if (true) response.ok.json(UpdateOpResp("success", "")) else response.ok.json(UpdateOpResp("fail", "user feedback fail!"))
    }
  }

}
